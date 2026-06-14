package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.RankedRecommendationQuote
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.RecommendationResult
import org.springframework.stereotype.Component

@Component
class RecommendationResultComposer(
    private val metadataScorer: MetadataScorer,
    private val recommendationRanker: RecommendationRanker,
    private val fallbackService: RecommendationFallbackService,
) {
    fun compose(
        input: RecommendationInput,
        effectiveTags: List<EffectiveTag>,
        candidates: List<RecommendationCandidate>,
        moodTagIdByCode: Map<String, Long>,
    ): RecommendationResult? {
        val initialRankedQuotes = rank(input, effectiveTags, candidates, moodTagIdByCode)
        val supplementedCandidates =
            fallbackService.supplementCandidates(
                effectiveTags = effectiveTags,
                existingCandidates = candidates,
                rankedQuotes = initialRankedQuotes,
            )
        val rankedQuotes =
            rank(input, effectiveTags, supplementedCandidates, moodTagIdByCode)
                .diversifyRoleTags()
                .take(RECOMMENDATION_RESULT_COUNT)
                .rerank()
        if (rankedQuotes.isEmpty()) return null

        return RecommendationResult(
            mainQuote = rankedQuotes.first(),
            quotes = rankedQuotes,
        )
    }

    private fun rank(
        input: RecommendationInput,
        effectiveTags: List<EffectiveTag>,
        candidates: List<RecommendationCandidate>,
        moodTagIdByCode: Map<String, Long>,
    ): List<RankedRecommendationQuote> =
        recommendationRanker.rank(candidates) { candidate ->
            metadataScorer.score(
                input = input,
                effectiveTags = effectiveTags,
                candidate = candidate,
                moodTagIdByCode = moodTagIdByCode,
            )
        }

    private fun List<RankedRecommendationQuote>.diversifyRoleTags(): List<RankedRecommendationQuote> {
        val selectedQuotes = mutableListOf<RankedRecommendationQuote>()
        val deferredQuotes = mutableListOf<RankedRecommendationQuote>()
        val roleTagCounts = mutableMapOf<Long, Int>()

        forEach { quote ->
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
        const val MAX_SAME_ROLE_TAG_COUNT = 3
        const val NO_ROLE_TAG_COUNT = 0
    }
}
