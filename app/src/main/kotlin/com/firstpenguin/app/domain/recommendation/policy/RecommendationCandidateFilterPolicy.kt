package com.firstpenguin.app.domain.recommendation.policy

import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.global.enums.TagType

object RecommendationCandidateFilterPolicy {
    fun hardFilterTags(effectiveTags: Collection<EffectiveTag>): List<EffectiveTag> {
        val emotionTags = effectiveTags.only(TagType.EMOTION)
        if (emotionTags.isNotEmpty()) return emotionTags

        return effectiveTags.only(TagType.NEED)
    }

    fun hardFilterTagKeys(effectiveTags: Collection<EffectiveTag>): List<Pair<TagType, Long>> =
        hardFilterTags(effectiveTags)
            .map { tag -> tag.type to tag.tagId }
            .sortedWith(compareBy<Pair<TagType, Long>> { (tagType) -> tagType.ordinal }.thenBy { (_, tagId) -> tagId })

    private fun Collection<EffectiveTag>.only(tagType: TagType): List<EffectiveTag> =
        filter { tag -> tag.type == tagType }
            .distinctBy { tag -> tag.tagId }
}
