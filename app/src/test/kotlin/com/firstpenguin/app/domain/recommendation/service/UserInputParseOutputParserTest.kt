package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.emotion.model.Tag
import com.firstpenguin.app.domain.quotemetadata.dto.TagOption
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.TagCandidatePriority
import com.firstpenguin.app.domain.recommendation.model.TagCandidateSource
import com.firstpenguin.app.global.enums.TagType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDateTime

class UserInputParseOutputParserTest {
    private val outputParser =
        UserInputParseOutputParser(
            objectMapper = JsonMapper.builder().build(),
            tagCandidateMapper = UserInputTagCandidateMapper(),
        )

    @Test
    fun `tag output JSON을 기존 TagCandidate 모델로 변환한다`() {
        val result =
            outputParser.parse(
                outputText(candidate("SITUATION_FAILURE_MISTAKE", TagType.SITUATION)),
                input,
                tagGroups,
            )
        val candidate = result.tagCandidates.first()

        assertEquals(null, result.canonicalIntent)
        assertEquals(SITUATION_TAG_ID, candidate.tagId)
        assertEquals("SITUATION_FAILURE_MISTAKE", candidate.code)
        assertEquals(TagType.SITUATION, candidate.type)
        assertEquals(TagCandidateSource.FEELING_TEXT, candidate.source)
        assertEquals(TagCandidatePriority.PRIMARY, candidate.priority)
    }

    @Test
    fun `selected tag와 중복된 LLM 후보는 제외한다`() {
        val result =
            outputParser.parse(
                outputText(candidate("NEED_COMFORT", TagType.NEED)),
                input.copy(needTag = tag(NEED_TAG_ID, TagType.NEED, "NEED_COMFORT")),
                tagGroups,
            )

        assertEquals(emptyList<Any>(), result.tagCandidates)
    }

    @Test
    fun `selected needTag가 있으면 LLM need 후보는 secondary 이하로 낮춘다`() {
        val result =
            outputParser.parse(
                outputText(candidate("NEED_PERSPECTIVE_SHIFT", TagType.NEED)),
                input.copy(needTag = tag(NEED_TAG_ID, TagType.NEED, "NEED_COMFORT")),
                tagGroups,
            )

        assertEquals(TagCandidatePriority.SECONDARY, result.tagCandidates.first().priority)
    }

    @Test
    fun `needTag가 없고 feelingText 기반 NEED 후보면 primary를 유지한다`() {
        val result =
            outputParser.parse(
                outputText(candidate("NEED_COMFORT", TagType.NEED)),
                input.copy(needTag = null),
                tagGroups,
            )

        assertEquals(TagCandidatePriority.PRIMARY, result.tagCandidates.first().priority)
    }

    @Test
    fun `LLM emotion 후보는 약한 보조 신호로 낮춘다`() {
        val result =
            outputParser.parse(
                outputText(candidate("EMOTION_ANXIOUS", TagType.EMOTION)),
                input,
                tagGroups,
            )

        assertEquals(TagCandidatePriority.SECONDARY, result.tagCandidates.first().priority)
    }

    @Test
    fun `지원하지 않는 tagType 후보는 제외한다`() {
        val result =
            outputParser.parse(
                outputText(
                    candidate("MOOD_CALM", TagType.MOOD),
                ),
                input,
                tagGroups,
            )

        assertFalse(result.tagCandidates.any())
    }

    private companion object {
        const val EMOTION_RANGE_ID = 1L
        const val EMOTION_VALUE = 1
        const val NEED_TAG_ID = 10L
        const val NEED_PERSPECTIVE_TAG_ID = 11L
        const val EMOTION_TAG_ID = 20L
        const val SITUATION_TAG_ID = 30L
        const val CONTEXT_TAG_ID = 40L
        const val MOOD_TAG_ID = 50L
        val CREATED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 14, 0, 0)
        val input =
            RecommendationInput(
                userId = 1L,
                emotionValue = EMOTION_VALUE,
                emotionRangeId = EMOTION_RANGE_ID,
                emotionTags = emptyList(),
                needTag = null,
                feelingText = "불안해서 위로받고 싶어",
                diaryText = null,
                analysis = null,
            )
        val tagGroups: Map<TagType, List<TagOption>> =
            mapOf(
                TagType.NEED to
                    listOf(
                        tagOption(NEED_TAG_ID, TagType.NEED, "NEED_COMFORT"),
                        tagOption(NEED_PERSPECTIVE_TAG_ID, TagType.NEED, "NEED_PERSPECTIVE_SHIFT"),
                    ),
                TagType.EMOTION to listOf(tagOption(EMOTION_TAG_ID, TagType.EMOTION, "EMOTION_ANXIOUS")),
                TagType.SITUATION to
                    listOf(
                        tagOption(SITUATION_TAG_ID, TagType.SITUATION, "SITUATION_FAILURE_MISTAKE"),
                    ),
                TagType.CONTEXT to listOf(tagOption(CONTEXT_TAG_ID, TagType.CONTEXT, "CONTEXT_RAIN")),
                TagType.MOOD to listOf(tagOption(MOOD_TAG_ID, TagType.MOOD, "MOOD_CALM")),
            )

        fun outputText(vararg candidates: Pair<TagType, String>): String =
            """
            {
              "emotionTagCandidates": [${candidates.jsonArray(TagType.EMOTION)}],
              "needTagCandidates": [${candidates.jsonArray(TagType.NEED)}],
              "situationTagCandidates": [${candidates.jsonArray(TagType.SITUATION)}],
              "contextTagCandidates": [${candidates.jsonArray(TagType.CONTEXT)}],
              "roleTagCandidates": [${candidates.jsonArray(TagType.ROLE)}]
            }
            """.trimIndent()

        fun candidate(
            code: String,
            type: TagType,
        ): Pair<TagType, String> =
            type to
                """
                {
                  "tagCode": "$code",
                  "source": "FEELING_TEXT"
                }
                """.trimIndent()

        fun Array<out Pair<TagType, String>>.jsonArray(type: TagType): String =
            filter { candidate -> candidate.first == type }
                .joinToString(",\n") { candidate -> candidate.second }

        fun tag(
            id: Long,
            type: TagType,
            code: String,
        ): Tag =
            Tag(
                id = id,
                emotionRangeId = if (type == TagType.EMOTION) EMOTION_RANGE_ID else null,
                code = code,
                label = code,
                type = type,
                createdAt = CREATED_AT,
            )

        fun tagOption(
            id: Long,
            type: TagType,
            code: String,
        ): TagOption =
            TagOption(
                id = id,
                type = type,
                code = code,
                label = code,
                description = null,
            )
    }
}
