package com.firstpenguin.app.domain.recommendation.model

@Suppress("MagicNumber")
object EffectiveTagPolicy {
    val sourceWeights: Map<TagCandidateSource, Double> =
        mapOf(
            TagCandidateSource.USER_SELECTED to 1.0,
            TagCandidateSource.FEELING_TEXT to 0.85,
            TagCandidateSource.DIARY_TEXT to 0.55,
        )

    val priorityWeights: Map<TagCandidatePriority, Double> =
        mapOf(
            TagCandidatePriority.PRIMARY to 1.0,
            TagCandidatePriority.SECONDARY to 0.65,
            TagCandidatePriority.BACKGROUND to 0.3,
        )

    fun effectiveImportance(candidate: TagCandidate): Double =
        sourceWeights.getValue(candidate.source) *
            priorityWeights.getValue(candidate.priority) *
            candidate.confidence.coerceIn(0.0, 1.0)

    fun toEffectiveTag(candidate: TagCandidate): EffectiveTag =
        EffectiveTag(
            tagId = candidate.tagId,
            type = candidate.type,
            weight = effectiveImportance(candidate),
        )
}
