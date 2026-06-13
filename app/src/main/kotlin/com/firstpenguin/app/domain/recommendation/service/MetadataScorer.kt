package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.IntentType
import com.firstpenguin.app.domain.recommendation.model.RankedRecommendationQuote
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.RecommendationResult
import com.firstpenguin.app.domain.recommendation.model.RecommendationScoreBreakdown
import com.firstpenguin.app.domain.recommendation.policy.IntentFocusWeightPolicy
import com.firstpenguin.app.domain.recommendation.policy.MoodTagPolicy
import com.firstpenguin.app.global.enums.TagType
import org.springframework.stereotype.Component

@Component
class MetadataScorer(
    private val moodTagPolicy: MoodTagPolicy,
    private val typeScoreCalculator: TypeScoreCalculator,
) {
    fun recommend(
        input: RecommendationInput,
        effectiveTags: List<EffectiveTag>,
        candidates: List<RecommendationCandidate>,
        moodTagIdByCode: Map<String, Long>,
    ): RecommendationResult? {
        val rankedQuotes = rank(input, effectiveTags, candidates, moodTagIdByCode)
        if (rankedQuotes.isEmpty()) return null

        return RecommendationResult(
            mainQuote = rankedQuotes.first(),
            quotes = rankedQuotes,
        )
    }

    fun rank(
        input: RecommendationInput,
        effectiveTags: List<EffectiveTag>,
        candidates: List<RecommendationCandidate>,
        moodTagIdByCode: Map<String, Long>,
    ): List<RankedRecommendationQuote> =
        candidates
            .map { candidate -> candidate to score(input, effectiveTags, candidate, moodTagIdByCode) }
            .sortedWith(
                compareByDescending<Pair<RecommendationCandidate, RecommendationScoreBreakdown>> {
                    it.second.finalScore
                }.thenBy { it.first.quoteId },
            ).mapIndexed { index, (candidate, score) ->
                RankedRecommendationQuote(
                    rank = index + FIRST_RANK,
                    candidate = candidate,
                    score = score,
                )
            }

    fun score(
        input: RecommendationInput,
        effectiveTags: List<EffectiveTag>,
        candidate: RecommendationCandidate,
        moodTagIdByCode: Map<String, Long>,
    ): RecommendationScoreBreakdown {
        val intentType = input.analysis?.intentType ?: IntentType.EMOTION_NEED_BASED
        val needScore = typeScore(TagType.NEED, effectiveTags, candidate)
        val emotionScore = typeScore(TagType.EMOTION, effectiveTags, candidate)
        val contextScore = typeScore(TagType.CONTEXT, effectiveTags, candidate)
        val situationScore = typeScore(TagType.SITUATION, effectiveTags, candidate)
        val moodScore = moodScore(input, effectiveTags, candidate, moodTagIdByCode)
        val metadataScore =
            metadataScore(
                intentType = intentType,
                needScore = needScore,
                emotionScore = emotionScore,
                contextScore = contextScore,
                situationScore = situationScore,
                moodScore = moodScore,
            )

        return RecommendationScoreBreakdown(
            needScore = needScore,
            emotionScore = emotionScore,
            contextScore = contextScore,
            situationScore = situationScore,
            moodScore = moodScore,
            metadataScore = metadataScore,
            semanticScore = DEFAULT_SEMANTIC_SCORE,
            finalScore = metadataScore,
        )
    }

    private fun typeScore(
        tagType: TagType,
        effectiveTags: List<EffectiveTag>,
        candidate: RecommendationCandidate,
    ): Double =
        typeScoreCalculator.calculate(
            targetTags = effectiveTags.filter { tag -> tag.type == tagType },
            candidateTagIds = candidate.tagIds(tagType),
        )

    private fun moodScore(
        input: RecommendationInput,
        effectiveTags: List<EffectiveTag>,
        candidate: RecommendationCandidate,
        moodTagIdByCode: Map<String, Long>,
    ): Double {
        val targetMoodTagIds =
            moodTagPolicy
                .resolveMoodTagCodes(input, effectiveTags)
                .mapNotNullTo(mutableSetOf()) { code -> moodTagIdByCode[code] }

        return typeScoreCalculator.calculate(
            targetTagIds = targetMoodTagIds,
            candidateTagIds = candidate.tagIds(TagType.MOOD),
        )
    }

    private fun metadataScore(
        intentType: IntentType,
        needScore: Double,
        emotionScore: Double,
        contextScore: Double,
        situationScore: Double,
        moodScore: Double,
    ): Double {
        val scores =
            mapOf(
                TagType.NEED to needScore,
                TagType.EMOTION to emotionScore,
                TagType.CONTEXT to contextScore,
                TagType.SITUATION to situationScore,
                TagType.MOOD to moodScore,
            )
        val weights = IntentFocusWeightPolicy.weightsOf(intentType)

        return scores.entries.sumOf { (type, score) -> score * (weights[type] ?: NO_WEIGHT) }
    }

    private fun RecommendationCandidate.tagIds(tagType: TagType): Set<Long> = tagIdsByType[tagType].orEmpty()

    private companion object {
        const val FIRST_RANK = 1
        const val DEFAULT_SEMANTIC_SCORE = 0.0
        const val NO_WEIGHT = 0.0
    }
}
