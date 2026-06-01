package com.firstpenguin.app.domain.batch.service

import com.firstpenguin.app.domain.batch.dto.TagOption
import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.global.enums.QuoteMetadataBatchModelVersion
import com.firstpenguin.app.global.enums.TagPriority
import com.firstpenguin.app.global.enums.TagType
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

private const val TAG_MAX_ITEMS = 3

private val QUOTE_METADATA_PROMPT_GUIDE =
    """
    너는 감정 기반 문장 추천 서비스의 인용구 태그 분석기다.

    너의 역할은 책의 인용구/문장을 분석하여, 제공된 허용 태그 목록 안에서만 관련 태그를 선택하는 것이다.

    이 작업의 목적은 인용구를 추천 시스템에서 검색/랭킹할 수 있도록 메타데이터화하는 것이다.

    중요 규칙:
    1. 새로운 태그를 만들지 마라.
    2. 반드시 제공된 tagCode 중에서만 선택하라.
    3. 출력에는 tagCode만 사용하라.
    4. label과 description은 의미 이해용으로만 사용한다.
    5. evidence는 인용구에서 근거가 되는 짧은 표현을 그대로 적어라.
    6. 인용구에 직접 드러나지 않더라도, 문장이 강하게 어울리는 감정/상황/맥락이면 선택할 수 있다.
    7. 단, 근거가 약한 태그를 억지로 많이 붙이지 마라.
    8. confidence는 0.0~1.0 사이로 둔다.
    9. priority는 primary, secondary, background 중 하나만 사용한다.
    10. roleTag는 반드시 1개만 선택한다.
    11. emotionTag, needTag, contextTag, situationTag, moodTag, avoidTag는 각각 0~3개까지만 선택한다.
    12. 출력은 반드시 JSON schema를 따른다.
    13. JSON 외의 설명 문장은 출력하지 마라.

    [태그 기준]
    - emotionTag: 이 인용구가 어울리는 감정 상태
    - needTag: 이 인용구가 사용자에게 줄 수 있는 문장 역할 또는 필요
    - contextTag: 이 인용구가 어울리는 장소, 날씨, 시간, 활동, 장면
    - situationTag: 이 인용구가 어울리는 삶의 문제, 사건, 관계, 주제
    - moodTag: 이 인용구 자체의 분위기, 말투, 정서적 톤
    - avoidTag: 이 인용구가 특정 사용자 상태에서 부담이 될 수 있는 요소
    - roleTag: 추천 결과에서 이 인용구가 담당할 대표 역할

    [roleTag 선택 기준]
    roleTag는 반드시 아래 관점 중 가장 강한 역할 1개만 선택한다.

    - 지금 마음을 알아주는 문장: 감정을 인정하거나 공감하는 문장
    - 생각을 바꿔주는 문장: 관점 전환, 성찰, 마음 정리를 돕는 문장
    - 다시 움직이게 하는 문장: 용기, 재시작, 행동으로 이어지게 하는 문장

    [priority 기준]
    - primary: 인용구의 핵심 의미와 직접적으로 맞는 태그
    - secondary: 핵심은 아니지만 충분히 관련 있는 태그
    - background: 약하게 어울리는 배경성 태그

    [embeddingText 작성 기준]
    embeddingText는 인용구 원문을 추천 검색에 잘 걸리도록 설명한 짧은 문장이다.
    아래 정보를 자연스럽게 포함하라.

    - 인용구의 핵심 의미
    - 어울리는 감정
    - 어울리는 필요/역할
    - 어울리는 상황이나 맥락
    - 문장의 분위기
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
                            "input" to buildPrompt(quote, tagGroups),
                            "text" to
                                mapOf(
                                    "format" to
                                        quoteMetadataSchema(
                                            quote = quote,
                                            tagGroups = tagGroups,
                                        ),
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
                            "roleTag",
                            "emotionTags",
                            "needTags",
                            "contextTags",
                            "situationTags",
                            "moodTags",
                            "avoidTags",
                            "embeddingText",
                        ),
                    "properties" to
                        mapOf(
                            "quoteId" to
                                mapOf(
                                    "type" to "string",
                                    "enum" to listOf(quote.id.toString()),
                                ),
                            "roleTag" to
                                roleTagResultSchema(tagGroups.getValue(TagType.ROLE)),
                            "emotionTags" to tagResultArraySchema(tagGroups.getValue(TagType.EMOTION)),
                            "needTags" to tagResultArraySchema(tagGroups.getValue(TagType.NEED)),
                            "contextTags" to tagResultArraySchema(tagGroups.getValue(TagType.CONTEXT)),
                            "situationTags" to tagResultArraySchema(tagGroups.getValue(TagType.SITUATION)),
                            "moodTags" to tagResultArraySchema(tagGroups.getValue(TagType.MOOD)),
                            "avoidTags" to tagResultArraySchema(tagGroups.getValue(TagType.AVOID)),
                            "embeddingText" to mapOf("type" to "string"),
                        ),
                ),
        )

    private fun roleTagResultSchema(options: List<TagOption>): Map<String, Any> =
        tagResultSchema(
            options = options,
            priorityCodes = listOf(TagPriority.PRIMARY.code),
        )

    private fun tagResultArraySchema(options: List<TagOption>): Map<String, Any> =
        mapOf(
            "type" to "array",
            "maxItems" to TAG_MAX_ITEMS,
            "items" to tagResultSchema(options),
        )

    private fun tagResultSchema(
        options: List<TagOption>,
        priorityCodes: List<String> = TagPriority.codes(),
    ): Map<String, Any> =
        mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "required" to
                listOf(
                    "tagCode",
                    "evidence",
                    "confidence",
                    "priority",
                ),
            "properties" to
                mapOf(
                    "tagCode" to
                        mapOf(
                            "type" to "string",
                            "enum" to options.map { option -> option.code },
                        ),
                    "evidence" to mapOf("type" to "string"),
                    "confidence" to
                        mapOf(
                            "type" to "number",
                            "minimum" to 0.0,
                            "maximum" to 1.0,
                        ),
                    "priority" to
                        mapOf(
                            "type" to "string",
                            "enum" to priorityCodes,
                        ),
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

    private fun tagOptionText(option: TagOption): String =
        "- tagCode: ${option.code}, label: ${option.label}, description: ${option.description.orEmpty()}"
}
