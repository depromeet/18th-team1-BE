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
        prioritizeSemanticFallback: Boolean = false,
    ): List<SourcedRecommendationCandidate> =
        supplementSourcedCandidates(
            input = input,
            effectiveTags = effectiveTags,
            existingCandidates = existingCandidates.map { candidate -> candidate.toSourcedPrimaryCandidate() },
            rankedQuotes = rankedQuotes,
            minimumCandidateCount = minimumCandidateCount,
            semanticCandidates = semanticCandidates,
            prioritizeSemanticFallback = prioritizeSemanticFallback,
        )

    fun supplementSourcedCandidates(
        input: RecommendationInput,
        effectiveTags: List<EffectiveTag>,
        existingCandidates: List<SourcedRecommendationCandidate>,
        rankedQuotes: List<RankedRecommendationQuote> = emptyList(),
        minimumCandidateCount: Int = MINIMUM_CANDIDATE_COUNT,
        semanticCandidates: () -> List<RecommendationCandidate> = { emptyList() },
        prioritizeSemanticFallback: Boolean = false,
    ): List<SourcedRecommendationCandidate> {
        val forceFallback = rankedQuotes.hasLowTopScore()

        val accumulator = CandidateAccumulator(existingCandidates)
        if (!forceFallback && accumulator.isEnough(minimumCandidateCount)) return accumulator.toList()

        val fallbackSteps = fallbackSteps(input, effectiveTags, semanticCandidates, prioritizeSemanticFallback)

        fallbackSteps
            .takeUntilEnough(
                accumulator = accumulator,
                minimumCandidateCount = minimumCandidateCount,
                forcedFallbackStepCount = forceFallback.stepCount(),
            ).forEach { step -> accumulator.add(step.findCandidates(), step.source) }

        return accumulator.toList()
    }

    private fun fallbackSteps(
        input: RecommendationInput,
        effectiveTags: List<EffectiveTag>,
        semanticCandidates: () -> List<RecommendationCandidate>,
        prioritizeSemanticFallback: Boolean,
    ): List<FallbackStep> {
        val emotionStep =
            fallbackStep(RecommendationCandidateSource.FALLBACK_EMOTION) {
                candidateProvider.findCandidatesByEmotionRangeId(input.emotionRangeId, FALLBACK_FETCH_LIMIT)
            }
        val semanticStep = fallbackStep(RecommendationCandidateSource.FALLBACK_SEMANTIC, semanticCandidates)
        val firstSteps =
            if (prioritizeSemanticFallback) {
                listOf(semanticStep, emotionStep)
            } else {
                listOf(emotionStep, semanticStep)
            }

        return firstSteps +
            listOf(
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
    }

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
        forcedFallbackStepCount: Int,
    ): Sequence<FallbackStep> =
        sequence {
            for ((index, step) in this@takeUntilEnough.withIndex()) {
                if (index >= forcedFallbackStepCount && accumulator.isEnough(minimumCandidateCount)) break
                yield(step)
            }
        }

    private fun Boolean.stepCount(): Int =
        if (this) {
            FORCED_FALLBACK_STEP_COUNT
        } else {
            NO_FORCED_FALLBACK_STEP_COUNT
        }

    private companion object {
        const val MINIMUM_CANDIDATE_COUNT = 10
        const val FALLBACK_FETCH_LIMIT = 300
        const val LOW_TOP_SCORE_THRESHOLD = 0.35
        const val NO_FORCED_FALLBACK_STEP_COUNT = 0
        const val FORCED_FALLBACK_STEP_COUNT = 2
    }
}

private data class FallbackStep(
    val source: RecommendationCandidateSource,
    val findCandidates: () -> List<RecommendationCandidate>,
)

private fun RecommendationCandidate.toSourcedPrimaryCandidate(): SourcedRecommendationCandidate =
    SourcedRecommendationCandidate(candidate = this, source = RecommendationCandidateSource.PRIMARY)

private class CandidateAccumulator(
    initialCandidates: List<SourcedRecommendationCandidate>,
) {
    private val candidatesByQuoteId = linkedMapOf<Long, SourcedRecommendationCandidate>()

    init {
        add(initialCandidates)
    }

    fun add(candidates: List<SourcedRecommendationCandidate>) {
        candidates.forEach { candidate -> candidatesByQuoteId.putIfAbsent(candidate.quoteId, candidate) }
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

    fun toList(): List<SourcedRecommendationCandidate> = candidatesByQuoteId.values.toList()
}
