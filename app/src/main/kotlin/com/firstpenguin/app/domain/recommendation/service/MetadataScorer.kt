package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.IntentType
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.RecommendationScoreBreakdown
import com.firstpenguin.app.domain.recommendation.policy.IntentFocusWeightPolicy
import com.firstpenguin.app.domain.recommendation.policy.IntentTypePolicy
import com.firstpenguin.app.domain.recommendation.policy.MoodTagPolicy
import com.firstpenguin.app.global.enums.TagType
import org.springframework.stereotype.Component

@Component
class MetadataScorer(
    private val moodTagPolicy: MoodTagPolicy,
    private val typeScoreCalculator: TypeScoreCalculator,
) {
    fun score(
        input: RecommendationInput,
        effectiveTags: List<EffectiveTag>,
        candidate: RecommendationCandidate,
        moodTagIdByCode: Map<String, Long>,
        tagRarityWeights: Map<Long, Double> = emptyMap(),
    ): RecommendationScoreBreakdown {
        val intentType = IntentTypePolicy.resolve(input, effectiveTags)
        val needScore = typeScore(TagType.NEED, effectiveTags, candidate, tagRarityWeights)
        val emotionScore = typeScore(TagType.EMOTION, effectiveTags, candidate, tagRarityWeights)
        val contextScore = typeScore(TagType.CONTEXT, effectiveTags, candidate)
        val situationScore = typeScore(TagType.SITUATION, effectiveTags, candidate)
        val moodScore = moodScore(input, effectiveTags, candidate, moodTagIdByCode, tagRarityWeights, intentType)
        val metadataScore =
            metadataScore(
                input = input,
                intentType = intentType,
                needScore = needScore,
                emotionScore = emotionScore,
                contextScore = contextScore,
                situationScore = situationScore,
                moodScore = moodScore,
            ) * missingEmotionPenalty(effectiveTags, emotionScore) *
                unsupportedSpecificityPenalty(effectiveTags, candidate)

        return RecommendationScoreBreakdown(
            needScore = needScore,
            emotionScore = emotionScore,
            contextScore = contextScore,
            situationScore = situationScore,
            moodScore = moodScore,
            metadataScore = metadataScore,
            semanticScore = DEFAULT_SEMANTIC_SCORE,
            finalScore = DEFAULT_FINAL_SCORE,
        )
    }

    private fun typeScore(
        tagType: TagType,
        effectiveTags: List<EffectiveTag>,
        candidate: RecommendationCandidate,
        tagRarityWeights: Map<Long, Double> = emptyMap(),
    ): Double =
        typeScoreCalculator.calculate(
            targetTags = effectiveTags.filter { tag -> tag.type == tagType },
            candidateTagIds = candidate.tagIds(tagType),
            tagRarityWeights = tagRarityWeights,
        )

    private fun moodScore(
        input: RecommendationInput,
        effectiveTags: List<EffectiveTag>,
        candidate: RecommendationCandidate,
        moodTagIdByCode: Map<String, Long>,
        tagRarityWeights: Map<Long, Double>,
        intentType: IntentType,
    ): Double {
        val targetMoodTagIds =
            moodTagPolicy
                .resolveMoodTagCodes(input, effectiveTags, intentType)
                .mapNotNullTo(mutableSetOf()) { code -> moodTagIdByCode[code] }

        return typeScoreCalculator.calculate(
            targetTagIds = targetMoodTagIds,
            candidateTagIds = candidate.tagIds(TagType.MOOD),
            tagRarityWeights = tagRarityWeights,
        )
    }

    private fun metadataScore(
        input: RecommendationInput,
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
        val weights = IntentFocusWeightPolicy.weightsOf(input, intentType)

        return scores.entries.sumOf { (type, score) -> score * (weights[type] ?: NO_WEIGHT) }
    }

    private fun missingEmotionPenalty(
        effectiveTags: List<EffectiveTag>,
        emotionScore: Double,
    ): Double =
        when {
            emotionScore > NO_SCORE -> NO_PENALTY
            effectiveTags.none { tag -> tag.type == TagType.EMOTION } -> NO_PENALTY
            else -> MISSING_EMOTION_PENALTY
        }

    private fun unsupportedSpecificityPenalty(
        effectiveTags: List<EffectiveTag>,
        candidate: RecommendationCandidate,
    ): Double =
        unsupportedSituationPenalty(effectiveTags, candidate) *
            unsupportedContextPenalty(effectiveTags, candidate)

    private fun unsupportedSituationPenalty(
        effectiveTags: List<EffectiveTag>,
        candidate: RecommendationCandidate,
    ): Double =
        unsupportedTagPenalty(
            effectiveTags = effectiveTags,
            candidate = candidate,
            tagType = TagType.SITUATION,
            penalty = UNSUPPORTED_SITUATION_PENALTY,
        )

    private fun unsupportedContextPenalty(
        effectiveTags: List<EffectiveTag>,
        candidate: RecommendationCandidate,
    ): Double =
        unsupportedTagPenalty(
            effectiveTags = effectiveTags,
            candidate = candidate,
            tagType = TagType.CONTEXT,
            penalty = UNSUPPORTED_CONTEXT_PENALTY,
        )

    private fun unsupportedTagPenalty(
        effectiveTags: List<EffectiveTag>,
        candidate: RecommendationCandidate,
        tagType: TagType,
        penalty: Double,
    ): Double =
        when {
            effectiveTags.any { tag -> tag.type == tagType } -> NO_PENALTY
            candidate.tagIds(tagType).isEmpty() -> NO_PENALTY
            else -> penalty
        }

    private fun RecommendationCandidate.tagIds(tagType: TagType): Set<Long> = tagIdsByType[tagType].orEmpty()

    private companion object {
        const val DEFAULT_SEMANTIC_SCORE = 0.0
        const val DEFAULT_FINAL_SCORE = 0.0
        const val NO_WEIGHT = 0.0
        const val NO_SCORE = 0.0
        const val NO_PENALTY = 1.0
        const val MISSING_EMOTION_PENALTY = 0.5
        const val UNSUPPORTED_SITUATION_PENALTY = 0.9
        const val UNSUPPORTED_CONTEXT_PENALTY = 0.95
    }
}
