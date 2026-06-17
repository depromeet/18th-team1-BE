package com.firstpenguin.app.domain.recommendation.policy

import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.IntentType
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.global.enums.TagType

object IntentTypePolicy {
    fun resolve(
        input: RecommendationInput,
        effectiveTags: Collection<EffectiveTag>,
    ): IntentType {
        val hasContext = effectiveTags.hasType(TagType.CONTEXT)
        val hasSituation = effectiveTags.hasType(TagType.SITUATION)
        val hasEmotionNeed = input.hasSelectedEmotionNeed() || effectiveTags.hasEmotionNeed()

        return when {
            hasContext && hasSituation -> IntentType.MIXED
            hasContext -> IntentType.CONTEXT_BASED
            hasSituation -> IntentType.SITUATION_BASED
            hasEmotionNeed -> IntentType.EMOTION_NEED_BASED
            else -> IntentType.MIXED
        }
    }

    private fun RecommendationInput.hasSelectedEmotionNeed(): Boolean {
        if (emotionTags.isNotEmpty()) return true

        return needTag != null
    }

    private fun Collection<EffectiveTag>.hasEmotionNeed(): Boolean = any { tag -> tag.type in EMOTION_NEED_TAG_TYPES }

    private fun Collection<EffectiveTag>.hasType(tagType: TagType): Boolean = any { tag -> tag.type == tagType }

    private val EMOTION_NEED_TAG_TYPES = setOf(TagType.EMOTION, TagType.NEED)
}
