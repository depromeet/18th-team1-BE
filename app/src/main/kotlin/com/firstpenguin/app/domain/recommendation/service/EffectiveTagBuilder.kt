package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.policy.EffectiveTagPolicy
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import org.springframework.stereotype.Component

@Component
class EffectiveTagBuilder {
    fun build(input: RecommendationInput): List<EffectiveTag> =
        (selectedTags(input) + analyzedTags(input))
            .groupBy { tag -> tag.type to tag.tagId }
            .map { (_, tags) -> mergeTags(tags) }
            .filter(EffectiveTagPolicy::isEffective)
            .sortedWith(compareBy<EffectiveTag> { tag -> tag.type.ordinal }.thenBy { tag -> tag.tagId })

    private fun selectedTags(input: RecommendationInput): List<EffectiveTag> =
        input.emotionTags.map(EffectiveTagPolicy::toSelectedEffectiveTag) +
            listOfNotNull(input.needTag?.let(EffectiveTagPolicy::toSelectedEffectiveTag))

    private fun analyzedTags(input: RecommendationInput): List<EffectiveTag> =
        input.analysis
            ?.tagCandidates
            ?.map(EffectiveTagPolicy::toEffectiveTag)
            .orEmpty()

    private fun mergeTags(tags: List<EffectiveTag>): EffectiveTag {
        val first = tags.first()

        return EffectiveTag(
            tagId = first.tagId,
            code = first.code,
            type = first.type,
            importance = EffectiveTagPolicy.mergeImportance(tags.map { tag -> tag.importance }),
        )
    }
}
