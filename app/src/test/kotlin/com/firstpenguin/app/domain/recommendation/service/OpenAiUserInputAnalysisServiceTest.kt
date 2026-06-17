package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.emotion.model.Tag
import com.firstpenguin.app.domain.emotion.repository.TagRepository
import com.firstpenguin.app.domain.emotion.repository.table.TagTable
import com.firstpenguin.app.domain.openai.dto.OpenAiResponsesRequest
import com.firstpenguin.app.domain.openai.dto.OpenAiTextResponse
import com.firstpenguin.app.domain.openai.service.OpenAiResponsesClient
import com.firstpenguin.app.domain.recommendation.model.IntentType
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.global.enums.TagType
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.Result
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDateTime

class OpenAiUserInputAnalysisServiceTest {
    @Test
    fun `feelingText와 diaryText가 모두 없으면 LLM을 호출하지 않는다`() {
        val openAi = openAiResponsesClient()
        val service = service(client = openAi.client)

        val result = service.analyze(recommendationInput(feelingText = null, diaryText = null))

        assertNull(result)
        Mockito.verifyNoInteractions(openAi.client)
    }

    @Test
    fun `free text가 있으면 LLM 응답을 UserInputAnalysis로 변환한다`() {
        val openAi = openAiResponsesClient(outputText = outputText)
        val service = service(client = openAi.client)

        val result = service.analyze(recommendationInput(feelingText = "비 오는 출근길에 마음이 불안해"))

        requireNotNull(result)
        assertEquals(IntentType.CONTEXT_BASED, result.intentType)
        assertEquals("비 오는 출근길에 불안한 마음을 차분히 다독이고 싶다", result.canonicalIntent)
        assertEquals(listOf(NEED_TAG_ID, CONTEXT_TAG_ID), result.tagCandidates.map { candidate -> candidate.tagId })
        assertEquals(USER_INPUT_ANALYSIS_MODEL, result.llmModel)
        assertEquals(1, result.llmModelVersion)
        assertEquals(OPEN_AI_INPUT_TOKENS, result.inputTokens)
        assertEquals(OPEN_AI_CACHED_TOKENS, result.cachedTokens)
        assertEquals(OPEN_AI_OUTPUT_TOKENS, result.outputTokens)
        assertTrue(openAi.lastRequest.input.contains("비 오는 출근길"))
        assertFalse(openAi.lastRequest.input.contains("hasSelectedNeedTag"))
        assertEquals(USER_INPUT_ANALYSIS_MODEL, openAi.lastRequest.model)
        assertEquals("user-input-analysis-v1-gpt-5-mini", openAi.lastRequest.promptCacheKey)
        assertEquals("24h", openAi.lastRequest.promptCacheRetention)
        assertTrue(
            openAi.lastRequest.text.format
                .toString()
                .contains("CONTEXT_RAIN"),
        )
    }

    @Test
    fun `사용자가 선택한 태그는 LLM prompt payload에 포함하지 않는다`() {
        val openAi = openAiResponsesClient(outputText = outputText)
        val service = service(client = openAi.client)

        service.analyze(
            recommendationInput(
                feelingText = null,
                diaryText = "비 오는 출근길에 마음이 불안해서 오래 생각이 많아졌다",
                emotionTags = listOf(tag(100L, TagType.EMOTION, "EMOTION_SELECTED")),
                needTag = tag(101L, TagType.NEED, "NEED_SELECTED"),
            ),
        )

        assertFalsePromptContainsSelectedTags(openAi.lastRequest.input)
        assertFalse(openAi.lastRequest.input.contains("hasSelectedNeedTag"))
    }

    @Test
    fun `allowedTags는 사용자 입력보다 앞에 배치한다`() {
        val openAi = openAiResponsesClient(outputText = outputText)
        val service = service(client = openAi.client)

        service.analyze(recommendationInput(feelingText = "비 오는 출근길에 마음이 불안해"))

        assertTrue(openAi.lastRequest.input.indexOf("[허용 태그 목록]") < openAi.lastRequest.input.indexOf("[분석 대상 사용자 입력]"))
        assertTrue(openAi.lastRequest.input.indexOf("CONTEXT_RAIN") < openAi.lastRequest.input.indexOf("비 오는 출근길"))
    }

    @Test
    fun `needTag가 있고 diaryText가 짧아도 LLM을 호출한다`() {
        val openAi = openAiResponsesClient(outputText = outputText)
        val service = service(client = openAi.client)

        val result =
            service.analyze(
                recommendationInput(
                    feelingText = null,
                    diaryText = "오늘 힘들다",
                    needTag = tag(NEED_TAG_ID, TagType.NEED, "NEED_COMFORT"),
                ),
            )

        requireNotNull(result)
        assertTrue(openAi.lastRequest.input.contains("오늘 힘들다"))
        assertFalse(openAi.lastRequest.input.contains("hasSelectedNeedTag"))
        assertEquals(USER_INPUT_ANALYSIS_MODEL, openAi.lastRequest.model)
    }

    @Test
    fun `needTag가 있어도 diaryText가 충분하면 LLM을 호출한다`() {
        val openAi = openAiResponsesClient(outputText = outputText)
        val service = service(client = openAi.client)

        val result =
            service.analyze(
                recommendationInput(
                    feelingText = null,
                    diaryText = "비 오는 출근길에 마음이 불안해서 오래 생각이 많아졌다",
                    needTag = tag(NEED_TAG_ID, TagType.NEED, "NEED_COMFORT"),
                ),
            )

        requireNotNull(result)
        assertTrue(openAi.lastRequest.input.contains("비 오는 출근길"))
        assertEquals(USER_INPUT_ANALYSIS_MODEL, openAi.lastRequest.model)
    }

    @Test
    fun `diaryText 길이와 관계없이 mini 모델을 사용한다`() {
        val openAi = openAiResponsesClient(outputText = outputText)
        val service = service(client = openAi.client)

        val result =
            service.analyze(
                recommendationInput(
                    feelingText = null,
                    diaryText = LONG_DIARY_TEXT,
                    needTag = tag(NEED_TAG_ID, TagType.NEED, "NEED_COMFORT"),
                ),
            )

        requireNotNull(result)
        assertEquals(USER_INPUT_ANALYSIS_MODEL, openAi.lastRequest.model)
    }

    @Test
    fun `LLM 호출이나 파싱이 실패하면 분석 결과를 버린다`() {
        val openAi = openAiResponsesClient(exception = IllegalStateException("failed"))
        val service = service(client = openAi.client)

        val result = service.analyze(recommendationInput(feelingText = "마음이 복잡해"))

        assertNull(result)
    }

    private companion object {
        const val EMOTION_RANGE_ID = 1L
        const val NEED_TAG_ID = 10L
        const val CONTEXT_TAG_ID = 20L
        const val USER_INPUT_ANALYSIS_MODEL = "gpt-5-mini"
        const val OPEN_AI_INPUT_TOKENS = 120L
        const val OPEN_AI_CACHED_TOKENS = 80L
        const val OPEN_AI_OUTPUT_TOKENS = 40L
        const val LONG_DIARY_TEXT: String =
            "비 오는 출근길에 마음이 불안해서 오래 생각이 많아졌다. " +
                "회사에서 실수한 일이 계속 떠오르고 관계도 어색해져서 마음을 정리하고 싶다. " +
                "집에 돌아와서도 그 장면이 반복해서 떠올라 쉽게 잠들 수 없었다."
        val CREATED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 14, 0, 0)
        val outputText: String =
            """
            {
              "intentType": "CONTEXT_BASED",
              "canonicalIntent": "비 오는 출근길에 불안한 마음을 차분히 다독이고 싶다",
              "emotionTagCandidates": [],
              "needTagCandidates": [
                {
                  "tagCode": "NEED_COMFORT",
                  "source": "FEELING_TEXT",
                  "priority": "PRIMARY",
                  "confidence": 0.88
                }
              ],
              "situationTagCandidates": [],
              "contextTagCandidates": [
                {
                  "tagCode": "CONTEXT_RAIN",
                  "source": "FEELING_TEXT",
                  "priority": "PRIMARY",
                  "confidence": 0.92
                }
              ],
              "roleTagCandidates": []
            }
            """.trimIndent()

        fun service(client: OpenAiResponsesClient): OpenAiUserInputAnalysisService =
            OpenAiUserInputAnalysisService(
                tagRepository = TagRepository(dslWithTags()),
                requestBuilder = UserInputParseRequestBuilder(JsonMapper.builder().build()),
                outputParser =
                    UserInputParseOutputParser(
                        objectMapper = JsonMapper.builder().build(),
                        tagCandidateMapper = UserInputTagCandidateMapper(),
                    ),
                openAiResponsesClient = client,
            )

        fun openAiResponsesClient(
            outputText: String = "",
            exception: RuntimeException? = null,
        ): OpenAiResponsesClientStub {
            val stub = OpenAiResponsesClientStub()
            val client =
                Mockito.mock(OpenAiResponsesClient::class.java) { invocation ->
                    if (invocation.method.name != "createTextResponse") {
                        return@mock Mockito.RETURNS_DEFAULTS.answer(invocation)
                    }

                    stub.lastRequest = invocation.getArgument(0)

                    exception?.let { throw it }
                    OpenAiTextResponse(
                        outputText = outputText,
                        inputTokens = OPEN_AI_INPUT_TOKENS,
                        cachedTokens = OPEN_AI_CACHED_TOKENS,
                        outputTokens = OPEN_AI_OUTPUT_TOKENS,
                    )
                }
            stub.client = client

            return stub
        }

        class OpenAiResponsesClientStub(
            var lastRequest: OpenAiResponsesRequest = emptyOpenAiResponsesRequest(),
        ) {
            lateinit var client: OpenAiResponsesClient
        }

        fun emptyOpenAiResponsesRequest(): OpenAiResponsesRequest =
            OpenAiResponsesRequest(
                model = "",
                reasoning = emptyMap(),
                input = "",
                text =
                    com.firstpenguin.app.domain.openai.dto.OpenAiResponsesTextRequest(
                        format = emptyMap(),
                        verbosity = "",
                    ),
            )

        fun dslWithTags(): DSLContext {
            lateinit var dsl: DSLContext
            val connection =
                MockConnection(
                    MockDataProvider {
                        arrayOf(MockResult(TAG_ROW_COUNT, tagResult(dsl)))
                    },
                )
            dsl = DSL.using(connection, SQLDialect.POSTGRES)

            return dsl
        }

        fun tagResult(dsl: DSLContext): Result<Record> =
            dsl.newResult(*TAG_FIELDS).apply {
                add(tagRecord(dsl, 1L, TagType.EMOTION, "EMOTION_ANXIOUS"))
                add(tagRecord(dsl, NEED_TAG_ID, TagType.NEED, "NEED_COMFORT"))
                add(tagRecord(dsl, 11L, TagType.SITUATION, "SITUATION_FAILURE_MISTAKE"))
                add(tagRecord(dsl, CONTEXT_TAG_ID, TagType.CONTEXT, "CONTEXT_RAIN"))
                add(tagRecord(dsl, 30L, TagType.ROLE, "ROLE_EMPATHY"))
            }

        fun tagRecord(
            dsl: DSLContext,
            id: Long,
            type: TagType,
            code: String,
        ): Record =
            dsl.newRecord(*TAG_FIELDS).apply {
                set(TagTable.ID, id)
                set(TagTable.TYPE, type.name)
                set(TagTable.CODE, code)
                set(TagTable.LABEL, code)
                set(TagTable.DESCRIPTION, code)
            }

        fun recommendationInput(
            feelingText: String?,
            diaryText: String? = null,
            emotionTags: List<Tag> = emptyList(),
            needTag: Tag? = null,
        ): RecommendationInput =
            RecommendationInput(
                userId = 1L,
                emotionRangeId = EMOTION_RANGE_ID,
                emotionTags = emotionTags,
                needTag = needTag,
                feelingText = feelingText,
                diaryText = diaryText,
                analysis = null,
            )

        fun assertFalsePromptContainsSelectedTags(input: String) {
            assertFalse(input.contains("selectedEmotionTagCodes"))
            assertFalse(input.contains("selectedNeedTagCode"))
            assertFalse(input.contains("hasSelectedNeedTag"))
            assertFalse(input.contains("EMOTION_SELECTED"))
            assertFalse(input.contains("NEED_SELECTED"))
        }

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

        const val TAG_ROW_COUNT = 5
        val TAG_FIELDS: Array<Field<*>> =
            arrayOf(
                TagTable.ID,
                TagTable.TYPE,
                TagTable.CODE,
                TagTable.LABEL,
                TagTable.DESCRIPTION,
            )
    }
}
