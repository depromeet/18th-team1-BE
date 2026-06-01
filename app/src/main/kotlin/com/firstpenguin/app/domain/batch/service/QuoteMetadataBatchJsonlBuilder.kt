package com.firstpenguin.app.domain.batch.service

import com.firstpenguin.app.domain.batch.dto.TagOption
import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.global.enums.QuoteMetadataBatchModelVersion
import com.firstpenguin.app.global.enums.TagType
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

private const val TAG_MAX_ITEMS = 3
private const val EMBEDDING_TEXT_MAX_LENGTH = 120

private val QUOTE_METADATA_PROMPT_GUIDE =
    """
    너는 감정 기반 문장 추천 서비스의 인용구 태그 분석기다.

    너의 역할은 책의 인용구/문장을 분석하여, 제공된 허용 태그 목록 안에서만 관련 태그를 선택하는 것이다.

    이 작업의 목적은 인용구를 추천 시스템에서 검색/랭킹할 수 있도록 메타데이터화하는 것이다.

    중요 규칙:
    1. 새로운 태그를 만들지 마라.
    2. 반드시 제공된 tagCode 중에서만 선택하라.
    3. 각 태그 배열은 최대 3개까지만 선택하라.
    4. label과 description은 의미 이해용으로만 사용한다.
    5. roleTagCode는 반드시 1개만 선택하라.
    6. 인용구에 직접 드러나지 않더라도, 문장이 강하게 어울리는 감정/상황/맥락이면 선택할 수 있다.
    7. 근거가 약한 태그를 억지로 붙이지 마라.
    8. embeddingText는 80자 이내 한국어 한 문장으로 작성하라.
    9. JSON schema만 반환하라.

    [태그 기준]
    - emotionTagCodes: 이 인용구가 어울리는 감정 상태
    - needTagCodes: 이 인용구가 사용자에게 줄 수 있는 문장 역할 또는 필요
    - contextTagCodes: 이 인용구가 어울리는 장소, 날씨, 시간, 활동, 장면
    - situationTagCodes: 이 인용구가 어울리는 삶의 문제, 사건, 관계, 주제
    - moodTagCodes: 이 인용구 자체의 분위기, 말투, 정서적 톤
    - avoidTagCodes: 이 인용구가 특정 사용자 상태에서 부담이 될 수 있는 요소
    - roleTagCode: 추천 결과에서 이 인용구가 담당할 대표 역할

    [roleTagCode 선택 기준]
    아래 세 관점 중 가장 강한 역할 1개만 선택한다.

    - 지금 마음을 알아주는 문장: 감정을 인정하거나 공감하는 문장
    - 생각을 바꿔주는 문장: 관점 전환, 성찰, 마음 정리를 돕는 문장
    - 다시 움직이게 하는 문장: 용기, 재시작, 행동으로 이어지게 하는 문장
    """.trimIndent()

@Component
class QuoteMetadataBatchJsonlBuilder(
    private val jsonMapper: JsonMapper,
) {
    fun build(
        quotes: List<Quote>,
        tagGroups: Map<TagType, List<TagOption>>,
    ): String =
        quotes.joinToString("\n") { quote ->
            jsonMapper.writeValueAsString(
                mapOf(
                    "custom_id" to "quote-${quote.id}",
                    "method" to "POST",
                    "url" to "/v1/responses",
                    "body" to
                        mapOf(
                            "model" to QuoteMetadataBatchModelVersion.V1.model,
                            "reasoning" to
                                mapOf(
                                    "effort" to "minimal",
                                ),
                            "input" to buildPrompt(quote, tagGroups),
                            "text" to
                                mapOf(
                                    "format" to
                                        quoteMetadataSchema(
                                            quote = quote,
                                            tagGroups = tagGroups,
                                        ),
                                    "verbosity" to "low",
                                ),
                        ),
                ),
            )
        }

    private fun quoteMetadataSchema(
        quote: Quote,
        tagGroups: Map<TagType, List<TagOption>>,
    ): Map<String, Any> =
        mapOf(
            "type" to "json_schema",
            "name" to "quote_metadata",
            "strict" to true,
            "schema" to
                mapOf(
                    "type" to "object",
                    "additionalProperties" to false,
                    "required" to
                        listOf(
                            "quoteId",
                            "roleTagCode",
                            "emotionTagCodes",
                            "needTagCodes",
                            "situationTagCodes",
                            "contextTagCodes",
                            "moodTagCodes",
                            "avoidTagCodes",
                            "embeddingText",
                        ),
                    "properties" to
                        mapOf(
                            "quoteId" to
                                mapOf(
                                    "type" to "integer",
                                    "enum" to listOf(quote.id),
                                ),
                            "roleTagCode" to codeSchema(tagGroups.getValue(TagType.ROLE)),
                            "emotionTagCodes" to codeArraySchema(tagGroups.getValue(TagType.EMOTION)),
                            "needTagCodes" to codeArraySchema(tagGroups.getValue(TagType.NEED)),
                            "situationTagCodes" to codeArraySchema(tagGroups.getValue(TagType.SITUATION)),
                            "contextTagCodes" to codeArraySchema(tagGroups.getValue(TagType.CONTEXT)),
                            "moodTagCodes" to codeArraySchema(tagGroups.getValue(TagType.MOOD)),
                            "avoidTagCodes" to codeArraySchema(tagGroups.getValue(TagType.AVOID)),
                            "embeddingText" to
                                mapOf(
                                    "type" to "string",
                                    "maxLength" to EMBEDDING_TEXT_MAX_LENGTH,
                                ),
                        ),
                ),
        )

    private fun codeSchema(options: List<TagOption>): Map<String, Any> =
        mapOf(
            "type" to "string",
            "enum" to options.map { option -> option.code },
        )

    private fun codeArraySchema(options: List<TagOption>): Map<String, Any> =
        mapOf(
            "type" to "array",
            "maxItems" to TAG_MAX_ITEMS,
            "items" to
                mapOf(
                    "type" to "string",
                    "enum" to options.map { option -> option.code },
                ),
        )

    private fun buildPrompt(
        quote: Quote,
        tagGroups: Map<TagType, List<TagOption>>,
    ): String =
        listOf(
            QUOTE_METADATA_PROMPT_GUIDE,
            allowedTagsText(tagGroups),
            quoteText(quote),
        ).joinToString("\n\n")

    private fun allowedTagsText(tagGroups: Map<TagType, List<TagOption>>): String =
        """
        [허용 태그 목록]
        아래 태그 목록 안에서만 tagCode를 선택해라.

        allowedEmotionTags:
        ${tagOptionsText(tagGroups.getValue(TagType.EMOTION))}

        allowedNeedTags:
        ${tagOptionsText(tagGroups.getValue(TagType.NEED))}

        allowedContextTags:
        ${tagOptionsText(tagGroups.getValue(TagType.CONTEXT))}

        allowedSituationTags:
        ${tagOptionsText(tagGroups.getValue(TagType.SITUATION))}

        allowedMoodTags:
        ${tagOptionsText(tagGroups.getValue(TagType.MOOD))}

        allowedAvoidTags:
        ${tagOptionsText(tagGroups.getValue(TagType.AVOID))}

        allowedRoleTags:
        ${tagOptionsText(tagGroups.getValue(TagType.ROLE))}
        """.trimIndent()

    private fun quoteText(quote: Quote): String =
        """
        [분석 대상 인용구]
        quoteId: ${quote.id}
        content: ${quote.content}
        """.trimIndent()

    private fun tagOptionsText(options: List<TagOption>): String = options.joinTags()

    private fun List<TagOption>.joinTags(): String = joinToString(separator = "\n", transform = ::tagOptionText)

    private fun tagOptionText(option: TagOption): String {
        val description = option.description.orEmpty()
        return "- ${option.code}: ${option.label} - $description"
    }
}
