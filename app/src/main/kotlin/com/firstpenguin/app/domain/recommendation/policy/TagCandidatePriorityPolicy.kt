package com.firstpenguin.app.domain.recommendation.policy

import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.TagCandidatePriority
import com.firstpenguin.app.domain.recommendation.model.TagCandidateSource
import com.firstpenguin.app.global.enums.TagType

object TagCandidatePriorityPolicy {
    fun resolve(
        input: RecommendationInput,
        tagType: TagType,
        source: TagCandidateSource,
    ): TagCandidatePriority =
        when (tagType) {
            TagType.NEED -> needPriority(input, source)
            TagType.SITUATION, TagType.CONTEXT -> supportingPriority(input, source)
            else -> TagCandidatePriority.SECONDARY
        }

    private fun needPriority(
        input: RecommendationInput,
        source: TagCandidateSource,
    ): TagCandidatePriority =
        if (input.needTag != null || source.isDiaryBackground(input)) {
            TagCandidatePriority.SECONDARY
        } else {
            TagCandidatePriority.PRIMARY
        }

    private fun supportingPriority(
        input: RecommendationInput,
        source: TagCandidateSource,
    ): TagCandidatePriority {
        if (source.isDiaryBackground(input)) return TagCandidatePriority.BACKGROUND

        return TagCandidatePriority.SECONDARY
    }

    private fun TagCandidateSource.isDiaryBackground(input: RecommendationInput): Boolean =
        this == TagCandidateSource.DIARY_TEXT && input.hasFeelingText()

    private fun RecommendationInput.hasFeelingText(): Boolean = !feelingText.isNullOrBlank()
}
