package com.firstpenguin.app.domain.batch.service

import com.firstpenguin.app.domain.batch.dto.TagOption
import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.global.enums.QuoteMetadataBatchModelVersion
import com.firstpenguin.app.global.enums.TagType
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

private const val TAG_MAX_ITEMS = 2
private const val CORE_TAG_MIN_ITEMS = 1
private const val STRICT_TAG_MAX_ITEMS = 1
private const val EMBEDDING_TEXT_MAX_LENGTH = 80

private val QUOTE_METADATA_PROMPT_GUIDE =
    """
    너는 감정 기반 문장 추천 서비스의 인용구 태그 분석기다.
    
    너의 역할은 책의 인용구/문장을 분석하여, 제공된 허용 태그 목록 안에서만 관련 태그를 선택하는 것이다.
    
    이 작업의 목적은 인용구를 추천 시스템에서 검색/랭킹할 수 있도록 메타데이터화하는 것이다.
    
    중요 규칙:
    1. 새로운 태그를 만들지 마라.
    2. 반드시 제공된 tagCode 중에서만 선택하라.
    3. label과 description은 태그 선택 기준이다. description의 선택/제외 조건을 우선한다.
    4. roleTagCode는 반드시 1개만 선택하라.
    5. emotionTagCodes, needTagCodes, moodTagCodes는 추천 매칭의 핵심 태그이므로 각각 1~2개를 선택하라.
    6. contextTagCodes, situationTagCodes, avoidTagCodes는 보조 태그이므로 description 기준에 명확히 맞을 때만 선택하라.
    7. 추상적 은유를 구체적인 장소, 사건, 직업, 학업, 관계 문제로 확장하지 마라.
    8. 표현이 단호하거나 희망적이라는 이유만으로 avoidTagCodes를 선택하지 마라.
    9. embeddingText는 50자 이내로, 핵심 의미와 필요/역할, 분위기만 담아라.
    10. 출력은 반드시 JSON schema를 따르고 JSON 외의 설명 문장은 출력하지 마라.
    
    [태그 기준]
    - emotionTagCodes: 이 인용구가 추천되기 좋은 사용자 마음 상태
    - needTagCodes: 이 인용구가 사용자에게 줄 수 있는 문장 역할 또는 필요
    - contextTagCodes: 인용구에 직접 드러나거나 매우 명확한 장소, 날씨, 시간, 활동, 장면
    - situationTagCodes: 인용구에 명확히 연결되는 삶의 문제, 사건, 관계, 주제
    - moodTagCodes: 이 인용구 자체의 분위기, 말투, 정서적 톤
    - avoidTagCodes: 이 인용구가 특정 사용자 상태에서 부담이 될 수 있는 표현 특성
    - roleTagCode: 추천 결과에서 이 인용구가 담당할 대표 역할
    
    [roleTagCode 선택 기준]
    아래 세 관점 중 가장 강한 역할 1개만 선택한다.
    
    - 지금 마음을 알아주는 문장: 감정을 인정하거나 공감하는 문장
    - 생각을 바꿔주는 문장: 관점 전환, 성찰, 마음 정리를 돕는 문장
    - 다시 움직이게 하는 문장: 용기, 재시작, 행동으로 이어지게 하는 문장
    
    [embeddingText 작성 기준]
    - 인용구 원문을 추천 검색에 잘 걸리도록 설명한 짧은 한국어 문장이다.
    - tagCode, label을 그대로 나열하지 마라.
    - “문장”, “느낌”, “촉구”, “필요할 때”, “어울리는” 같은 설명형 표현은 쓰지 마라.
    - context나 situation이 명확하지 않으면 장소/상황을 넣지 마라.
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
                            "emotionTagCodes" to coreCodeArraySchema(tagGroups.getValue(TagType.EMOTION)),
                            "needTagCodes" to coreCodeArraySchema(tagGroups.getValue(TagType.NEED)),
                            "situationTagCodes" to
                                codeArraySchema(
                                    options = tagGroups.getValue(TagType.SITUATION),
                                    maxItems = STRICT_TAG_MAX_ITEMS,
                                ),
                            "contextTagCodes" to
                                codeArraySchema(
                                    options = tagGroups.getValue(TagType.CONTEXT),
                                    maxItems = STRICT_TAG_MAX_ITEMS,
                                ),
                            "moodTagCodes" to coreCodeArraySchema(tagGroups.getValue(TagType.MOOD)),
                            "avoidTagCodes" to
                                codeArraySchema(
                                    options = tagGroups.getValue(TagType.AVOID),
                                    maxItems = STRICT_TAG_MAX_ITEMS,
                                ),
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

    private fun codeArraySchema(
        options: List<TagOption>,
        maxItems: Int = TAG_MAX_ITEMS,
        minItems: Int = 0,
    ): Map<String, Any> =
        mapOf(
            "type" to "array",
            "minItems" to minItems,
            "maxItems" to maxItems,
            "items" to
                mapOf(
                    "type" to "string",
                    "enum" to options.map { option -> option.code },
                ),
        )

    private fun coreCodeArraySchema(options: List<TagOption>): Map<String, Any> =
        codeArraySchema(
            options = options,
            minItems = CORE_TAG_MIN_ITEMS,
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
