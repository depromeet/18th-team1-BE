package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.RankedRecommendationQuote
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidateSource
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.SourcedRecommendationCandidate
import com.firstpenguin.app.domain.recommendation.repository.RecommendationCandidateProvider
import com.firstpenguin.app.global.enums.TagType
import org.springframework.stereotype.Component

@Component
class RecommendationFallbackService(
    private val candidateProvider: RecommendationCandidateProvider,
) {
    fun supplementCandidates(
        input: RecommendationInput,
        effectiveTags: List<EffectiveTag>,
        existingCandidates: List<RecommendationCandidate>,
        rankedQuotes: List<RankedRecommendationQuote> = emptyList(),
        minimumCandidateCount: Int = MINIMUM_CANDIDATE_COUNT,
        semanticCandidates: () -> List<RecommendationCandidate> = { emptyList() },
    ): List<SourcedRecommendationCandidate> {
        val forceFallback = rankedQuotes.hasLowTopScore()

        val accumulator = CandidateAccumulator(existingCandidates)
        if (!forceFallback && accumulator.isEnough(minimumCandidateCount)) return accumulator.toList()

        val fallbackSteps = fallbackSteps(input, effectiveTags, semanticCandidates)

        fallbackSteps
            .takeUntilEnough(
                accumulator = accumulator,
                minimumCandidateCount = minimumCandidateCount,
                forceFallback = forceFallback,
            ).forEach { step -> accumulator.add(step.findCandidates(), step.source) }

        return accumulator.toList()
    }

    private fun fallbackSteps(
        input: RecommendationInput,
        effectiveTags: List<EffectiveTag>,
        semanticCandidates: () -> List<RecommendationCandidate>,
    ): List<FallbackStep> =
        listOf(
            fallbackStep(RecommendationCandidateSource.FALLBACK_EMOTION) {
                candidateProvider.findCandidatesByEmotionRangeId(input.emotionRangeId, FALLBACK_FETCH_LIMIT)
            },
            fallbackStep(RecommendationCandidateSource.FALLBACK_SEMANTIC, semanticCandidates),
            fallbackStep(RecommendationCandidateSource.FALLBACK_NEED) {
                candidateProvider.findCandidates(effectiveTags.only(TagType.NEED), FALLBACK_FETCH_LIMIT)
            },
            fallbackStep(RecommendationCandidateSource.FALLBACK_RELAXED) {
                candidateProvider.findRelaxedCandidates(FALLBACK_FETCH_LIMIT)
            },
            fallbackStep(RecommendationCandidateSource.FALLBACK_RANDOM) {
                candidateProvider.findRandomCandidates(FALLBACK_FETCH_LIMIT)
            },
        )

    private fun fallbackStep(
        source: RecommendationCandidateSource,
        findCandidates: () -> List<RecommendationCandidate>,
    ): FallbackStep = FallbackStep(source = source, findCandidates = findCandidates)

    private fun List<RankedRecommendationQuote>.hasLowTopScore(): Boolean {
        val topScore = firstOrNull()?.score?.finalScore ?: return false

        return topScore < LOW_TOP_SCORE_THRESHOLD
    }

    private fun List<EffectiveTag>.only(tagType: TagType): List<EffectiveTag> = filter { tag -> tag.type == tagType }

    private fun List<FallbackStep>.takeUntilEnough(
        accumulator: CandidateAccumulator,
        minimumCandidateCount: Int,
        forceFallback: Boolean,
    ): Sequence<FallbackStep> =
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

private data class FallbackStep(
    val source: RecommendationCandidateSource,
    val findCandidates: () -> List<RecommendationCandidate>,
)

private class CandidateAccumulator(
    initialCandidates: List<RecommendationCandidate>,
) {
    private val candidatesByQuoteId = linkedMapOf<Long, SourcedRecommendationCandidate>()

    init {
        add(initialCandidates, RecommendationCandidateSource.PRIMARY)
    }

    fun add(
        candidates: List<RecommendationCandidate>,
        source: RecommendationCandidateSource,
    ) {
        candidates.forEach { candidate ->
            candidatesByQuoteId.putIfAbsent(
                candidate.quoteId,
                SourcedRecommendationCandidate(candidate = candidate, source = source),
            )
        }
    }

    fun isEnough(minimumCandidateCount: Int): Boolean = candidatesByQuoteId.size >= minimumCandidateCount

    fun isEnough(
        minimumCandidateCount: Int,
        forceFallback: Boolean,
    ): Boolean {
        if (forceFallback) return false

        return candidatesByQuoteId.size >= minimumCandidateCount
    }

    fun toList(): List<SourcedRecommendationCandidate> = candidatesByQuoteId.values.toList()
}
