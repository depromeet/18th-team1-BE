package com.firstpenguin.app.domain.recommendation.policy

import com.firstpenguin.app.domain.emotion.model.Tag
import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.TagCandidate
import com.firstpenguin.app.domain.recommendation.model.TagCandidatePriority
import com.firstpenguin.app.domain.recommendation.model.TagCandidateSource

@Suppress("MagicNumber")
object EffectiveTagPolicy {
    const val USER_SELECTED_IMPORTANCE = 1.0
    const val MIN_EFFECTIVE_IMPORTANCE = 0.15

    val sourceWeights: Map<TagCandidateSource, Double> =
        mapOf(
            TagCandidateSource.USER_SELECTED to USER_SELECTED_IMPORTANCE,
            TagCandidateSource.FEELING_TEXT to 0.85,
            TagCandidateSource.DIARY_TEXT to 0.55,
        )

    val priorityWeights: Map<TagCandidatePriority, Double> =
        mapOf(
            TagCandidatePriority.PRIMARY to 1.0,
            TagCandidatePriority.SECONDARY to 0.65,
            TagCandidatePriority.BACKGROUND to 0.3,
        )

    fun toSelectedEffectiveTag(tag: Tag): EffectiveTag =
        EffectiveTag(
            tagId = tag.id,
            code = tag.code,
            type = tag.type,
            importance = USER_SELECTED_IMPORTANCE,
        )

    fun effectiveImportance(candidate: TagCandidate): Double =
        sourceWeights.getValue(candidate.source) *
            priorityWeights.getValue(candidate.priority) *
            candidate.confidence.coerceIn(0.0, 1.0)

    fun mergeImportance(importances: List<Double>): Double =
        USER_SELECTED_IMPORTANCE -
            importances.fold(USER_SELECTED_IMPORTANCE) { remains, importance ->
                remains * (USER_SELECTED_IMPORTANCE - importance)
            }

    fun isEffective(tag: EffectiveTag): Boolean = tag.importance >= MIN_EFFECTIVE_IMPORTANCE

    fun toEffectiveTag(candidate: TagCandidate): EffectiveTag =
        EffectiveTag(
            tagId = candidate.tagId,
            code = candidate.code,
            type = candidate.type,
            importance = effectiveImportance(candidate),
        )
}