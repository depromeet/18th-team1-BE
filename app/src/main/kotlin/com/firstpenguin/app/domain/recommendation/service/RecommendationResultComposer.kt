package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.RankedRecommendationQuote
import com.firstpenguin.app.domain.recommendation.model.RecommendationAiModelVersion
import com.firstpenguin.app.domain.recommendation.model.RecommendationAnalysisLog
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidateSource
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.RecommendationResult
import com.firstpenguin.app.domain.recommendation.model.SourcedRecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.UserSemanticEmbedding
import com.firstpenguin.app.domain.recommendation.policy.RecommendationFinalScoreWeightPolicy
import com.firstpenguin.app.domain.recommendation.policy.RecommendationSemanticExpansionDecision
import com.firstpenguin.app.domain.recommendation.policy.RecommendationSemanticExpansionPolicy
import org.springframework.stereotype.Component

private const val FIRST_RANK = 1
private const val RECOMMENDATION_RESULT_COUNT = 10
private const val SCORE_PRIORITY_QUOTE_COUNT = 3
private const val MAX_SAME_ROLE_TAG_COUNT = 3
private const val NO_ROLE_TAG_COUNT = 0
private const val DEFAULT_SEMANTIC_SCORE = 0.0
private const val STRONG_SEMANTIC_SCORE = 0.55
private const val STRONG_SEMANTIC_METADATA_SCORE = 0.25

private typealias RankedQuotes = List<RankedRecommendationQuote>
private typealias SemanticExpansion = RecommendationSemanticExpansionDecision

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
        userEmbedding: UserSemanticEmbedding? = semanticProvider.prepare(input),
    ): RecommendationResult? =
        composeOrNull(
            input = input,
            effectiveTags = effectiveTags,
            candidates = candidates,
            moodTagIdByCode = moodTagIdByCode,
            tagRarityWeights = tagRarityWeights,
            userEmbedding = userEmbedding,
        )

    private fun composeOrNull(
        input: RecommendationInput,
        effectiveTags: List<EffectiveTag>,
        candidates: List<RecommendationCandidate>,
        moodTagIdByCode: Map<String, Long>,
        tagRarityWeights: Map<Long, Double>,
        userEmbedding: UserSemanticEmbedding?,
    ): RecommendationResult? {
        val primaryCandidates =
            sourceCandidates(
                candidates = candidates,
                source = RecommendationCandidateSource.PRIMARY,
            )
        val initialRankedQuotes =
            rank(input, effectiveTags, primaryCandidates, moodTagIdByCode, tagRarityWeights, userEmbedding)
        val semanticExpansion =
            RecommendationSemanticExpansionPolicy.decide(
                input = input,
                effectiveTags = effectiveTags,
                userEmbedding = userEmbedding,
            )
        val supplementedCandidates =
            findSupplementedCandidates(
                input = input,
                effectiveTags = effectiveTags,
                candidates = candidates,
                initialRankedQuotes = initialRankedQuotes,
                userEmbedding = userEmbedding,
                semanticExpansion = semanticExpansion,
            )
        val rankedQuotes =
            rank(input, effectiveTags, supplementedCandidates, moodTagIdByCode, tagRarityWeights, userEmbedding)
                .diversifyRoleTags()
                .preferEmotionMatches(semanticExpansion)
                .preferEmotionSources(semanticExpansion)
                .take(RECOMMENDATION_RESULT_COUNT)
                .rerank()
        if (rankedQuotes.isEmpty()) return null

        return RecommendationResult(
            mainQuote = rankedQuotes.first(),
            quotes = rankedQuotes,
            analysisLog = input.analysisLog(userEmbedding),
        )
    }

    private fun findSupplementedCandidates(
        input: RecommendationInput,
        effectiveTags: List<EffectiveTag>,
        candidates: List<RecommendationCandidate>,
        initialRankedQuotes: List<RankedRecommendationQuote>,
        userEmbedding: UserSemanticEmbedding?,
        semanticExpansion: SemanticExpansion,
    ): List<SourcedRecommendationCandidate> =
        semanticSeedCandidates(semanticExpansion, userEmbedding, candidates)
            .let { semanticSeeds ->
                val existingCandidates =
                    sourceCandidates(
                        candidates,
                        RecommendationCandidateSource.PRIMARY,
                    ) + semanticSeeds
                fallbackService.supplementSourcedCandidates(
                    input = input,
                    effectiveTags = effectiveTags,
                    existingCandidates = existingCandidates,
                    rankedQuotes = initialRankedQuotes,
                    semanticCandidates = {
                        semanticProvider.findSimilarCandidates(
                            userEmbedding = userEmbedding,
                            excludedQuoteIds = existingCandidates.map { candidate -> candidate.quoteId },
                        )
                    },
                    prioritizeSemanticFallback = semanticExpansion.prioritizeFallback,
                )
            }

    private fun semanticSeedCandidates(
        semanticExpansion: RecommendationSemanticExpansionDecision,
        userEmbedding: UserSemanticEmbedding?,
        candidates: List<RecommendationCandidate>,
    ): List<SourcedRecommendationCandidate> {
        val seedLimit = semanticExpansion.seedLimit ?: return emptyList()

        return semanticProvider
            .findSimilarCandidates(
                userEmbedding = userEmbedding,
                excludedQuoteIds =
                    candidates.map { candidate ->
                        candidate.quoteId
                    },
                limit = seedLimit,
            ).map { candidate ->
                SourcedRecommendationCandidate(
                    candidate = candidate,
                    source = RecommendationCandidateSource.FALLBACK_SEMANTIC,
                )
            }
    }

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
        val scoreWeights = RecommendationFinalScoreWeightPolicy.weightsOf(effectiveTags, recommendationCandidates)
        val semanticScores =
            semanticProvider.findScores(
                userEmbedding = userEmbedding,
                quoteIds = recommendationCandidates.map { candidate -> candidate.quoteId },
            )

        return recommendationRanker
            .rank(
                candidates = recommendationCandidates,
                useSemanticScore = semanticScores.isNotEmpty(),
                scoreWeights = scoreWeights,
            ) { candidate ->
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
}

private fun RecommendationInput.analysisLog(userEmbedding: UserSemanticEmbedding?): RecommendationAnalysisLog? {
    val analysis = analysis ?: return fallbackAnalysisLog(userEmbedding)

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

private fun RecommendationInput.fallbackAnalysisLog(userEmbedding: UserSemanticEmbedding?): RecommendationAnalysisLog? {
    if (!hasAnalysisText()) return null
    val modelVersion = RecommendationAiModelVersion.USER_INPUT_ANALYSIS_V1

    return RecommendationAnalysisLog(
        llmModel = modelVersion.model,
        llmModelVersion = modelVersion.version,
        canonicalIntent = null,
        embeddingInputText = userEmbedding?.inputText,
        inputTokens = null,
        cachedTokens = null,
        outputTokens = null,
        llmElapsedMs = null,
        embeddingElapsedMs = userEmbedding?.embeddingElapsedMs,
    )
}

private fun RecommendationInput.hasAnalysisText(): Boolean = feelingText.hasValue() || diaryText.hasValue()

private fun RankedQuotes.preferEmotionMatches(semanticExpansion: SemanticExpansion): RankedQuotes =
    filterNot { quote -> quote.shouldDeferForMissingEmotion(semanticExpansion) }
        .plus(filter { quote -> quote.shouldDeferForMissingEmotion(semanticExpansion) })
        .distinctBy { quote -> quote.quoteId }

private fun RankedQuotes.preferEmotionSources(semanticExpansion: SemanticExpansion): RankedQuotes =
    filter { quote -> quote.source.isEmotionSource() || quote.isStrongSemanticMatch(semanticExpansion) }
        .plus(filterNot { quote -> quote.source.isEmotionSource() || quote.isStrongSemanticMatch(semanticExpansion) })
        .distinctBy { quote -> quote.quoteId }

private fun RankedRecommendationQuote.shouldDeferForMissingEmotion(semanticExpansion: SemanticExpansion): Boolean =
    score.emotionScore <= NO_EMOTION_SCORE && !isStrongSemanticMatch(semanticExpansion)

private fun RankedRecommendationQuote.isStrongSemanticMatch(semanticExpansion: SemanticExpansion): Boolean =
    score.semanticScore >= STRONG_SEMANTIC_SCORE &&
        (
            !semanticExpansion.requireMetadataForStrongSemantic ||
                score.metadataScore >= STRONG_SEMANTIC_METADATA_SCORE
        )

private fun RecommendationCandidateSource.isEmotionSource(): Boolean =
    this == RecommendationCandidateSource.PRIMARY || this == RecommendationCandidateSource.FALLBACK_EMOTION

private fun String?.hasValue(): Boolean = !isNullOrBlank()

private const val NO_EMOTION_SCORE = 0.0
