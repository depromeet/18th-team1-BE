package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.RankedRecommendationQuote
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.repository.RecommendationCandidateProvider
import com.firstpenguin.app.global.enums.TagType
import org.springframework.stereotype.Component

@Component
class RecommendationFallbackService(
    private val candidateProvider: RecommendationCandidateProvider,
) {
    fun supplementCandidates(
        effectiveTags: List<EffectiveTag>,
        existingCandidates: List<RecommendationCandidate>,
        rankedQuotes: List<RankedRecommendationQuote> = emptyList(),
        minimumCandidateCount: Int = MINIMUM_CANDIDATE_COUNT,
    ): List<RecommendationCandidate> {
        val forceFallback = rankedQuotes.hasLowTopScore()
        if (!forceFallback && existingCandidates.size >= minimumCandidateCount) return existingCandidates

        val accumulator = CandidateAccumulator(existingCandidates)
        val fallbackSteps =
            listOf(
                { candidateProvider.findCandidates(effectiveTags.only(TagType.NEED), FALLBACK_FETCH_LIMIT) },
                { candidateProvider.findCandidates(effectiveTags.only(TagType.EMOTION), FALLBACK_FETCH_LIMIT) },
                { candidateProvider.findRelaxedCandidates(FALLBACK_FETCH_LIMIT) },
                { candidateProvider.findRandomCandidates(FALLBACK_FETCH_LIMIT) },
            )

        fallbackSteps
            .takeUntilEnough(accumulator, minimumCandidateCount, forceFallback)
            .forEach { findCandidates -> accumulator.add(findCandidates()) }

        return accumulator.toList()
    }

    private fun List<RankedRecommendationQuote>.hasLowTopScore(): Boolean {
        val topScore = firstOrNull()?.score?.finalScore ?: return false

        return topScore < LOW_TOP_SCORE_THRESHOLD
    }

    private fun List<EffectiveTag>.only(tagType: TagType): List<EffectiveTag> = filter { tag -> tag.type == tagType }

    private fun List<() -> List<RecommendationCandidate>>.takeUntilEnough(
        accumulator: CandidateAccumulator,
        minimumCandidateCount: Int,
        forceFallback: Boolean,
    ): Sequence<() -> List<RecommendationCandidate>> =
        sequence {
            for (step in this@takeUntilEnough) {
                if (accumulator.isEnough(minimumCandidateCount, forceFallback)) break
                yield(step)
            }
        }

    private companion object {
        const val MINIMUM_CANDIDATE_COUNT = 10
        const val FALLBACK_FETCH_LIMIT = 300
        const val LOW_TOP_SCORE_THRESHOLD = 0.35
    }
}

private class CandidateAccumulator(
    initialCandidates: List<RecommendationCandidate>,
) {
    private val candidatesByQuoteId = linkedMapOf<Long, RecommendationCandidate>()

    init {
        add(initialCandidates)
    }

    fun add(candidates: List<RecommendationCandidate>) {
        candidates.forEach { candidate ->
            candidatesByQuoteId.putIfAbsent(candidate.quoteId, candidate)
        }
    }

    fun isEnough(
        minimumCandidateCount: Int,
        forceFallback: Boolean,
    ): Boolean {
        if (forceFallback) return false

        return candidatesByQuoteId.size >= minimumCandidateCount
    }

    fun toList(): List<RecommendationCandidate> = candidatesByQuoteId.values.toList()
}
