package com.firstpenguin.app.domain.recommendation.policy

import com.firstpenguin.app.domain.emotion.model.Tag
import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.IntentType
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.global.enums.TagType
import org.springframework.stereotype.Component

@Suppress("MagicNumber")
@Component
class MoodTagPolicy {
    fun resolveMoodTagCodes(
        input: RecommendationInput,
        effectiveTags: List<EffectiveTag>,
    ): Set<String> {
        val intentType = input.analysis?.intentType ?: IntentType.EMOTION_NEED_BASED
        if (IntentFocusWeightPolicy.weightOf(intentType, TagType.MOOD) <= 0.0) return emptySet()

        return moodScores(input, effectiveTags)
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { it.key })
            .take(MAX_MOOD_TAG_COUNT)
            .mapTo(linkedSetOf()) { it.key }
    }

    private fun moodScores(
        input: RecommendationInput,
        effectiveTags: List<EffectiveTag>,
    ): Map<String, Double> {
        val scores = mutableMapOf<String, Double>()
        val selectedTagCodes = input.selectedTagCodes()

        scores.addRules(moodByNeedTag(input.needTag))
        input.emotionTags.forEach { tag ->
            scores.addRules(moodByEmotionCode(tag.code.normalizeCode(TagType.EMOTION)))
        }
        effectiveTags
            .filterNot { tag -> tag.normalizedCodeKey() in selectedTagCodes }
            .forEach { tag -> scores.addRules(moodByEffectiveTag(tag), tag.importance.coerceIn(0.0, 1.0)) }

        return scores
    }

    private fun MutableMap<String, Double>.addRules(
        rules: List<MoodRule>,
        multiplier: Double = 1.0,
    ) {
        rules.forEach { rule -> merge(rule.moodTagCode, rule.weight * multiplier, Double::plus) }
    }

    private fun moodByNeedTag(needTag: Tag?): List<MoodRule> =
        needTag
            ?.code
            ?.normalizeCode(TagType.NEED)
            ?.let { code -> MOOD_BY_NEED_CODE[code] }
            .orEmpty()

    private fun moodByEffectiveTag(tag: EffectiveTag): List<MoodRule> {
        val code = tag.code.normalizeCode(tag.type)

        return when (tag.type) {
            TagType.NEED -> MOOD_BY_NEED_CODE[code].orEmpty()
            TagType.EMOTION -> moodByEmotionCode(code)
            TagType.SITUATION -> MOOD_BY_SITUATION_CODE[code].orEmpty()
            TagType.CONTEXT -> MOOD_BY_CONTEXT_CODE[code].orEmpty()
            else -> emptyList()
        }
    }

    private fun moodByEmotionCode(code: String): List<MoodRule> =
        EMOTION_GROUP_BY_EMOTION_CODE[code]
            ?.let { group -> MOOD_BY_EMOTION_GROUP[group] }
            .orEmpty()

    private fun RecommendationInput.selectedTagCodes(): Set<Pair<TagType, String>> {
        val codes = emotionTags.mapTo(mutableSetOf()) { tag -> tag.type to tag.code.normalizeCode(tag.type) }
        needTag?.let { tag -> codes.add(tag.type to tag.code.normalizeCode(tag.type)) }

        return codes
    }

    private fun EffectiveTag.normalizedCodeKey(): Pair<TagType, String> = type to code.normalizeCode(type)

    private fun String.normalizeCode(type: TagType): String {
        val prefix = "${type.name}_"

        return if (startsWith(prefix)) this else prefix + this
    }

    private companion object {
        const val MAX_MOOD_TAG_COUNT = 5

        const val MOOD_PLAIN = "MOOD_PLAIN"
        const val MOOD_WARM = "MOOD_WARM"
        const val MOOD_REALISTIC = "MOOD_REALISTIC"
        const val MOOD_CALM_TONE = "MOOD_CALM_TONE"
        const val MOOD_GENTLE = "MOOD_GENTLE"
        const val MOOD_POETIC = "MOOD_POETIC"
        const val MOOD_AFFECTIONATE = "MOOD_AFFECTIONATE"
        const val MOOD_HOPEFUL = "MOOD_HOPEFUL"
        const val MOOD_LIGHT = "MOOD_LIGHT"

        val MOOD_BY_NEED_CODE: Map<String, List<MoodRule>> =
            mapOf(
                "NEED_COMFORT" to moodRules(MOOD_WARM to 1.0, MOOD_CALM_TONE to 0.8, MOOD_GENTLE to 0.6),
                "NEED_EMPATHY" to moodRules(MOOD_WARM to 1.0, MOOD_AFFECTIONATE to 0.8, MOOD_GENTLE to 0.6),
                "NEED_MIND_CLEARING" to moodRules(MOOD_CALM_TONE to 1.0, MOOD_PLAIN to 0.8, MOOD_GENTLE to 0.5),
                "NEED_PERSPECTIVE_SHIFT" to moodRules(MOOD_REALISTIC to 1.0, MOOD_PLAIN to 0.7, MOOD_CALM_TONE to 0.5),
                "NEED_COURAGE" to moodRules(MOOD_REALISTIC to 0.9, MOOD_HOPEFUL to 0.8, MOOD_WARM to 0.5),
                "NEED_SELF_ESTEEM" to moodRules(MOOD_WARM to 1.0, MOOD_REALISTIC to 0.7, MOOD_HOPEFUL to 0.6),
                "NEED_LETTING_GO" to moodRules(MOOD_CALM_TONE to 1.0, MOOD_GENTLE to 0.8, MOOD_PLAIN to 0.5),
                "NEED_RESTART" to moodRules(MOOD_HOPEFUL to 1.0, MOOD_REALISTIC to 0.8, MOOD_LIGHT to 0.5),
                "NEED_INSPIRATION" to moodRules(MOOD_POETIC to 1.0, MOOD_LIGHT to 0.7, MOOD_HOPEFUL to 0.4),
            )

        val EMOTION_GROUP_BY_EMOTION_CODE: Map<String, EmotionGroup> =
            mapOf(
                "EMOTION_ANXIOUS" to EmotionGroup.NEGATIVE_TENSION,
                "EMOTION_IRRITATED" to EmotionGroup.NEGATIVE_TENSION,
                "EMOTION_SENSITIVE" to EmotionGroup.NEGATIVE_TENSION,
                "EMOTION_WRONGED" to EmotionGroup.NEGATIVE_TENSION,
                "EMOTION_HURT" to EmotionGroup.NEGATIVE_TENSION,
                "EMOTION_UNSETTLED" to EmotionGroup.NEGATIVE_TENSION,
                "EMOTION_STUCK" to EmotionGroup.NEGATIVE_TENSION,
                "EMOTION_EXHAUSTED" to EmotionGroup.NEGATIVE_LOW_ENERGY,
                "EMOTION_UNMOTIVATED" to EmotionGroup.NEGATIVE_LOW_ENERGY,
                "EMOTION_TEARFUL" to EmotionGroup.NEGATIVE_LOW_ENERGY,
                "EMOTION_WITHDRAWN" to EmotionGroup.NEGATIVE_LOW_ENERGY,
                "EMOTION_SELF_BLAME" to EmotionGroup.NEGATIVE_LOW_ENERGY,
                "EMOTION_GIVE_UP" to EmotionGroup.NEGATIVE_LOW_ENERGY,
                "EMOTION_DEPRESSED" to EmotionGroup.NEGATIVE_LOW_ENERGY,
                "EMOTION_DISAPPOINTED" to EmotionGroup.NEGATIVE_LOW_ENERGY,
                "EMOTION_EMPTY" to EmotionGroup.NEGATIVE_LOW_ENERGY,
                "EMOTION_REGRETFUL" to EmotionGroup.NEGATIVE_LOW_ENERGY,
                "EMOTION_DAZED" to EmotionGroup.NEGATIVE_LOW_ENERGY,
                "EMOTION_NUMB" to EmotionGroup.NEGATIVE_LOW_ENERGY,
                "EMOTION_DROWSY" to EmotionGroup.NEGATIVE_LOW_ENERGY,
                "EMOTION_BORED" to EmotionGroup.NEUTRAL_LOW_STIMULUS,
                "EMOTION_WANT_TO_BE_ALONE" to EmotionGroup.NEUTRAL_LOW_STIMULUS,
                "EMOTION_CALM" to EmotionGroup.NEUTRAL_LOW_STIMULUS,
                "EMOTION_PEACEFUL" to EmotionGroup.NEUTRAL_LOW_STIMULUS,
                "EMOTION_THOUGHTFUL" to EmotionGroup.NEUTRAL_LOW_STIMULUS,
                "EMOTION_RELAXED" to EmotionGroup.NEUTRAL_LOW_STIMULUS,
                "EMOTION_ORDINARY" to EmotionGroup.NEUTRAL_LOW_STIMULUS,
                "EMOTION_AMBIVALENT" to EmotionGroup.NEUTRAL_LOW_STIMULUS,
                "EMOTION_SENTIMENTAL" to EmotionGroup.NEUTRAL_LOW_STIMULUS,
                "EMOTION_UNEVENTFUL" to EmotionGroup.NEUTRAL_LOW_STIMULUS,
                "EMOTION_GENEROUS" to EmotionGroup.POSITIVE_RELIEF_GRATITUDE,
                "EMOTION_PROUD" to EmotionGroup.POSITIVE_RELIEF_GRATITUDE,
                "EMOTION_SMILED" to EmotionGroup.POSITIVE_RELIEF_GRATITUDE,
                "EMOTION_LIGHTHEARTED" to EmotionGroup.POSITIVE_RELIEF_GRATITUDE,
                "EMOTION_SERENE" to EmotionGroup.POSITIVE_RELIEF_GRATITUDE,
                "EMOTION_GRATEFUL" to EmotionGroup.POSITIVE_RELIEF_GRATITUDE,
                "EMOTION_REASSURED" to EmotionGroup.POSITIVE_RELIEF_GRATITUDE,
                "EMOTION_FULFILLED" to EmotionGroup.POSITIVE_RELIEF_GRATITUDE,
                "EMOTION_MOVED" to EmotionGroup.POSITIVE_RELIEF_GRATITUDE,
                "EMOTION_NEW_BEGINNING" to EmotionGroup.POSITIVE_GROWTH_ENERGY,
                "EMOTION_HAPPY" to EmotionGroup.POSITIVE_GROWTH_ENERGY,
                "EMOTION_EXCITED" to EmotionGroup.POSITIVE_GROWTH_ENERGY,
                "EMOTION_FLUTTERING" to EmotionGroup.POSITIVE_GROWTH_ENERGY,
                "EMOTION_THRILLED" to EmotionGroup.POSITIVE_GROWTH_ENERGY,
                "EMOTION_ENERGIZED" to EmotionGroup.POSITIVE_GROWTH_ENERGY,
                "EMOTION_CONFIDENT" to EmotionGroup.POSITIVE_GROWTH_ENERGY,
                "EMOTION_PASSIONATE" to EmotionGroup.POSITIVE_GROWTH_ENERGY,
            )

        val MOOD_BY_EMOTION_GROUP: Map<EmotionGroup, List<MoodRule>> =
            mapOf(
                EmotionGroup.NEGATIVE_TENSION to
                    moodRules(MOOD_CALM_TONE to 0.8, MOOD_PLAIN to 0.7, MOOD_REALISTIC to 0.6, MOOD_WARM to 0.5),
                EmotionGroup.NEGATIVE_LOW_ENERGY to
                    moodRules(MOOD_WARM to 0.8, MOOD_CALM_TONE to 0.7, MOOD_GENTLE to 0.6, MOOD_PLAIN to 0.5),
                EmotionGroup.NEUTRAL_LOW_STIMULUS to
                    moodRules(MOOD_LIGHT to 0.7, MOOD_GENTLE to 0.6, MOOD_CALM_TONE to 0.6, MOOD_PLAIN to 0.5),
                EmotionGroup.POSITIVE_RELIEF_GRATITUDE to
                    moodRules(MOOD_GENTLE to 0.7, MOOD_WARM to 0.7, MOOD_POETIC to 0.5, MOOD_CALM_TONE to 0.5),
                EmotionGroup.POSITIVE_GROWTH_ENERGY to
                    moodRules(MOOD_HOPEFUL to 0.8, MOOD_REALISTIC to 0.6, MOOD_LIGHT to 0.5, MOOD_POETIC to 0.4),
            )

        val MOOD_BY_SITUATION_CODE: Map<String, List<MoodRule>> =
            mapOf(
                "SITUATION_FAILURE_MISTAKE" to
                    moodRules(MOOD_PLAIN to 0.7, MOOD_REALISTIC to 0.7, MOOD_WARM to 0.5, MOOD_HOPEFUL to 0.3),
                "SITUATION_WORK_CAREER" to moodRules(MOOD_REALISTIC to 0.7, MOOD_PLAIN to 0.6, MOOD_CALM_TONE to 0.4),
                "SITUATION_RELATIONSHIP" to
                    moodRules(MOOD_WARM to 0.7, MOOD_CALM_TONE to 0.5, MOOD_AFFECTIONATE to 0.4, MOOD_PLAIN to 0.4),
                "SITUATION_FRIEND" to moodRules(MOOD_WARM to 0.7, MOOD_AFFECTIONATE to 0.6, MOOD_CALM_TONE to 0.4),
                "SITUATION_FAMILY" to moodRules(MOOD_WARM to 0.7, MOOD_AFFECTIONATE to 0.6, MOOD_CALM_TONE to 0.4),
                "SITUATION_ROMANCE" to moodRules(MOOD_AFFECTIONATE to 0.8, MOOD_WARM to 0.6, MOOD_POETIC to 0.4),
                "SITUATION_BREAKUP" to moodRules(MOOD_WARM to 0.7, MOOD_CALM_TONE to 0.6, MOOD_PLAIN to 0.4),
                "SITUATION_FUTURE_CAREER" to
                    moodRules(
                        MOOD_HOPEFUL to 0.7,
                        MOOD_REALISTIC to 0.6,
                        MOOD_CALM_TONE to 0.4,
                    ),
                "SITUATION_STUDY" to moodRules(MOOD_REALISTIC to 0.6, MOOD_PLAIN to 0.5, MOOD_CALM_TONE to 0.4),
                "SITUATION_BURNOUT" to moodRules(MOOD_WARM to 0.7, MOOD_CALM_TONE to 0.6, MOOD_GENTLE to 0.6),
                "SITUATION_LOSS" to moodRules(MOOD_WARM to 0.8, MOOD_CALM_TONE to 0.7, MOOD_GENTLE to 0.5),
                "SITUATION_COMPARISON" to moodRules(MOOD_REALISTIC to 0.7, MOOD_WARM to 0.6, MOOD_PLAIN to 0.5),
            )

        val MOOD_BY_CONTEXT_CODE: Map<String, List<MoodRule>> =
            mapOf(
                "CONTEXT_RAIN" to
                    moodRules(
                        MOOD_CALM_TONE to 0.5,
                        MOOD_GENTLE to 0.5,
                        MOOD_POETIC to 0.4,
                        MOOD_WARM to 0.3,
                    ),
                "CONTEXT_CLOUDY" to moodRules(MOOD_CALM_TONE to 0.4, MOOD_GENTLE to 0.4, MOOD_PLAIN to 0.3),
                "CONTEXT_SNOW" to
                    moodRules(
                        MOOD_CALM_TONE to 0.5,
                        MOOD_GENTLE to 0.5,
                        MOOD_POETIC to 0.5,
                        MOOD_WARM to 0.3,
                    ),
                "CONTEXT_NIGHT" to
                    moodRules(
                        MOOD_CALM_TONE to 0.5,
                        MOOD_GENTLE to 0.5,
                        MOOD_PLAIN to 0.4,
                        MOOD_POETIC to 0.3,
                    ),
                "CONTEXT_DAWN" to
                    moodRules(
                        MOOD_GENTLE to 0.6,
                        MOOD_CALM_TONE to 0.5,
                        MOOD_POETIC to 0.4,
                        MOOD_HOPEFUL to 0.3,
                    ),
                "CONTEXT_BEFORE_SLEEP" to moodRules(MOOD_CALM_TONE to 0.6, MOOD_GENTLE to 0.6, MOOD_PLAIN to 0.3),
                "CONTEXT_WALKING" to moodRules(MOOD_LIGHT to 0.5, MOOD_PLAIN to 0.4, MOOD_CALM_TONE to 0.3),
                "CONTEXT_ALONE_TIME" to moodRules(MOOD_CALM_TONE to 0.5, MOOD_GENTLE to 0.5, MOOD_PLAIN to 0.3),
                "CONTEXT_RESTING" to moodRules(MOOD_CALM_TONE to 0.5, MOOD_GENTLE to 0.5),
                "CONTEXT_SUNNY" to moodRules(MOOD_HOPEFUL to 0.4, MOOD_LIGHT to 0.4, MOOD_WARM to 0.3),
                "CONTEXT_MORNING" to moodRules(MOOD_HOPEFUL to 0.5, MOOD_LIGHT to 0.4),
                "CONTEXT_EVENING" to moodRules(MOOD_CALM_TONE to 0.4, MOOD_POETIC to 0.4, MOOD_GENTLE to 0.3),
                "CONTEXT_SPRING" to moodRules(MOOD_HOPEFUL to 0.5, MOOD_LIGHT to 0.4, MOOD_WARM to 0.3),
                "CONTEXT_AUTUMN" to moodRules(MOOD_POETIC to 0.5, MOOD_CALM_TONE to 0.4, MOOD_GENTLE to 0.4),
                "CONTEXT_WINTER" to moodRules(MOOD_CALM_TONE to 0.4, MOOD_GENTLE to 0.4, MOOD_POETIC to 0.3),
                "CONTEXT_HOME" to moodRules(MOOD_WARM to 0.4, MOOD_CALM_TONE to 0.3),
                "CONTEXT_CAFE" to moodRules(MOOD_CALM_TONE to 0.3, MOOD_LIGHT to 0.3),
                "CONTEXT_RIVERSIDE" to moodRules(MOOD_CALM_TONE to 0.4, MOOD_POETIC to 0.4),
                "CONTEXT_SEA" to moodRules(MOOD_CALM_TONE to 0.4, MOOD_POETIC to 0.4),
                "CONTEXT_MOUNTAIN" to moodRules(MOOD_CALM_TONE to 0.4, MOOD_REALISTIC to 0.3),
                "CONTEXT_TRANSIT" to moodRules(MOOD_PLAIN to 0.3, MOOD_CALM_TONE to 0.3),
                "CONTEXT_MOVING" to moodRules(MOOD_PLAIN to 0.3, MOOD_LIGHT to 0.3),
                "CONTEXT_WAITING" to moodRules(MOOD_CALM_TONE to 0.3, MOOD_PLAIN to 0.3),
            )

        private fun moodRules(vararg rules: Pair<String, Double>) = rules.map { MoodRule(it.first, it.second) }
    }
}

private data class MoodRule(
    val moodTagCode: String,
    val weight: Double,
)

private enum class EmotionGroup {
    NEGATIVE_TENSION,
    NEGATIVE_LOW_ENERGY,
    NEUTRAL_LOW_STIMULUS,
    POSITIVE_RELIEF_GRATITUDE,
    POSITIVE_GROWTH_ENERGY,
}
