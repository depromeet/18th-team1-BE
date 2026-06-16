package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.RankedRecommendationQuote
import com.firstpenguin.app.domain.recommendation.model.RecommendationAnalysisLog
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidateSource
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.RecommendationResult
import com.firstpenguin.app.domain.recommendation.model.SourcedRecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.UserSemanticEmbedding
import org.springframework.stereotype.Component

@Component
class RecommendationResultComposer(
    private val metadataScorer: MetadataScorer,
    private val recommendationRanker: RecommendationRanker,
    private val fallbackService: RecommendationFallbackService,
    private val semanticProvider: RecommendationSemanticProvider,
) {
    fun compose(
        input: RecommendationInput,
        effectiveTags: List<EffectiveTag>,
        candidates: List<RecommendationCandidate>,
        moodTagIdByCode: Map<String, Long>,
        tagRarityWeights: Map<Long, Double> = emptyMap(),
    ): RecommendationResult? =
        composeOrNull(
            input = input,
            effectiveTags = effectiveTags,
            candidates = candidates,
            moodTagIdByCode = moodTagIdByCode,
            tagRarityWeights = tagRarityWeights,
        )

    private fun composeOrNull(
        input: RecommendationInput,
        effectiveTags: List<EffectiveTag>,
        candidates: List<RecommendationCandidate>,
        moodTagIdByCode: Map<String, Long>,
        tagRarityWeights: Map<Long, Double>,
    ): RecommendationResult? {
        val userEmbedding = semanticProvider.prepare(input)
        val primaryCandidates =
            sourceCandidates(
                candidates = candidates,
                source = RecommendationCandidateSource.PRIMARY,
            )
        val initialRankedQuotes =
            rank(input, effectiveTags, primaryCandidates, moodTagIdByCode, tagRarityWeights, userEmbedding)
        val supplementedCandidates =
            findSupplementedCandidates(
                effectiveTags = effectiveTags,
                candidates = candidates,
                initialRankedQuotes = initialRankedQuotes,
                userEmbedding = userEmbedding,
            )
        val rankedQuotes =
            rank(input, effectiveTags, supplementedCandidates, moodTagIdByCode, tagRarityWeights, userEmbedding)
                .diversifyRoleTags()
                .take(RECOMMENDATION_RESULT_COUNT)
                .rerank()
        if (rankedQuotes.isEmpty()) return null

        return RecommendationResult(
            mainQuote = rankedQuotes.first(),
            quotes = rankedQuotes,
            analysisLog = input.analysisLog(userEmbedding),
        )
    }

    private fun RecommendationInput.analysisLog(userEmbedding: UserSemanticEmbedding?): RecommendationAnalysisLog? {
        val analysis = analysis ?: return null

        return RecommendationAnalysisLog(
            llmModel = analysis.llmModel,
            llmModelVersion = analysis.llmModelVersion,
            canonicalIntent = analysis.canonicalIntent,
            embeddingInputText = userEmbedding?.inputText,
            inputTokens = analysis.inputTokens,
            cachedTokens = analysis.cachedTokens,
            outputTokens = analysis.outputTokens,
            llmElapsedMs = analysis.llmElapsedMs,
            embeddingElapsedMs = userEmbedding?.embeddingElapsedMs,
        )
    }

    private fun findSupplementedCandidates(
        effectiveTags: List<EffectiveTag>,
        candidates: List<RecommendationCandidate>,
        initialRankedQuotes: List<RankedRecommendationQuote>,
        userEmbedding: UserSemanticEmbedding?,
    ): List<SourcedRecommendationCandidate> =
        fallbackService.supplementCandidates(
            effectiveTags = effectiveTags,
            existingCandidates = candidates,
            rankedQuotes = initialRankedQuotes,
            semanticCandidates = {
                semanticProvider.findSimilarCandidates(
                    userEmbedding = userEmbedding,
                    excludedQuoteIds = candidates.map { candidate -> candidate.quoteId },
                )
            },
        )

    private fun rank(
        input: RecommendationInput,
        effectiveTags: List<EffectiveTag>,
        candidates: List<SourcedRecommendationCandidate>,
        moodTagIdByCode: Map<String, Long>,
        tagRarityWeights: Map<Long, Double>,
        userEmbedding: UserSemanticEmbedding?,
    ): List<RankedRecommendationQuote> {
        val recommendationCandidates = candidates.map { candidate -> candidate.candidate }
        val sourceByQuoteId = candidates.associate { candidate -> candidate.quoteId to candidate.source }
        val semanticScores =
            semanticProvider.findScores(
                userEmbedding = userEmbedding,
                quoteIds = recommendationCandidates.map { candidate -> candidate.quoteId },
            )

        return recommendationRanker
            .rank(recommendationCandidates) { candidate ->
                metadataScorer
                    .score(
                        input = input,
                        effectiveTags = effectiveTags,
                        candidate = candidate,
                        moodTagIdByCode = moodTagIdByCode,
                        tagRarityWeights = tagRarityWeights,
                    ).copy(semanticScore = semanticScores[candidate.quoteId] ?: DEFAULT_SEMANTIC_SCORE)
            }.map { quote ->
                quote.copy(source = sourceByQuoteId.getValue(quote.quoteId))
            }
    }

    private fun sourceCandidates(
        candidates: List<RecommendationCandidate>,
        source: RecommendationCandidateSource,
    ): List<SourcedRecommendationCandidate> =
        candidates.map { candidate -> SourcedRecommendationCandidate(candidate = candidate, source = source) }

    private fun List<RankedRecommendationQuote>.diversifyRoleTags(): List<RankedRecommendationQuote> {
        val scorePriorityQuotes = take(SCORE_PRIORITY_QUOTE_COUNT)
        val selectedQuotes = scorePriorityQuotes.toMutableList()
        val deferredQuotes = mutableListOf<RankedRecommendationQuote>()
        val roleTagCounts = scorePriorityQuotes.roleTagCounts()

        drop(SCORE_PRIORITY_QUOTE_COUNT).forEach { quote ->
            if (selectedQuotes.size >= RECOMMENDATION_RESULT_COUNT) return selectedQuotes
            if (quote.isRoleTagLimitExceeded(roleTagCounts)) {
                deferredQuotes.add(quote)
            } else {
                selectedQuotes.add(quote)
                quote.candidate.roleTagId?.let { roleTagId -> roleTagCounts.increase(roleTagId) }
            }
        }

        return selectedQuotes
            .plus(deferredQuotes)
            .distinctBy { quote -> quote.quoteId }
    }

    private fun List<RankedRecommendationQuote>.roleTagCounts(): MutableMap<Long, Int> =
        mapNotNull { quote -> quote.candidate.roleTagId }
            .groupingBy { roleTagId -> roleTagId }
            .eachCount()
            .toMutableMap()

    private fun RankedRecommendationQuote.isRoleTagLimitExceeded(roleTagCounts: Map<Long, Int>): Boolean {
        val roleTagId = candidate.roleTagId ?: return false

        return roleTagCounts.getOrDefault(roleTagId, NO_ROLE_TAG_COUNT) >= MAX_SAME_ROLE_TAG_COUNT
    }

    private fun MutableMap<Long, Int>.increase(roleTagId: Long) {
        this[roleTagId] = getOrDefault(roleTagId, NO_ROLE_TAG_COUNT) + 1
    }

    private fun List<RankedRecommendationQuote>.rerank(): List<RankedRecommendationQuote> =
        mapIndexed { index, quote -> quote.copy(rank = index + FIRST_RANK) }

    private companion object {
        const val FIRST_RANK = 1
        const val RECOMMENDATION_RESULT_COUNT = 10
        const val SCORE_PRIORITY_QUOTE_COUNT = 5
        const val MAX_SAME_ROLE_TAG_COUNT = 3
        const val NO_ROLE_TAG_COUNT = 0
        const val DEFAULT_SEMANTIC_SCORE = 0.0
    }
}
