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
    3. roleTagCode는 반드시 1개만 선택하라.
    4. emotionTagCodes, needTagCodes, contextTagCodes, situationTagCodes, moodTagCodes, avoidTagCodes는 각각 최대 2개까지만 선택하라.
    5. 태그를 억지로 채우지 마라. 관련성이 약하면 빈 배열을 반환하라.
    6. label과 description은 의미 이해용으로만 사용한다.
    7. emotionTagCodes, needTagCodes, moodTagCodes는 인용구의 핵심 의미와 강하게 맞는 경우에만 선택하라.
    8. contextTagCodes는 장소, 날씨, 시간, 활동, 장면이 인용구에 직접 드러나거나 매우 명확하게 연상될 때만 선택하라.
    9. situationTagCodes는 실패/실수, 일/커리어, 관계, 이별, 학업처럼 구체적인 삶의 문제나 사건이 명확할 때만 선택하라.
    10. 추상적 은유만으로 contextTagCodes나 situationTagCodes를 선택하지 마라.
    11. avoidTagCodes는 문장 자체가 훈계, 비난, 과한 긍정, 성공 압박처럼 특정 사용자 상태에서 부담이 될 수 있을 때만 선택하라.
    12. embeddingText는 80자 이내의 한국어 한 문장으로 작성하라.
    13. 출력은 반드시 JSON schema를 따른다.
    14. JSON 외의 설명 문장은 출력하지 마라.
    
    [태그 기준]
    - emotionTagCodes: 이 인용구가 강하게 어울리는 감정 상태
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
    
    [선택 기준 예시]
    - “새로운 시작”, “다시 나아감”, “껍질을 깨고 나옴”처럼 변화와 성장의 의미가 강하면 needTagCodes에는 용기/재시작 계열을 선택할 수 있다.
    - “자기 자신을 찾는 길”, “나를 이해하는 과정”처럼 성찰의 의미가 강하면 roleTagCode는 생각을 바꿔주는 문장에 가깝다.
    - “비”, “밤”, “바다”, “카페”, “출근길”처럼 장소/날씨/시간/활동이 직접 나오지 않으면 contextTagCodes는 비워도 된다.
    - “회사”, “시험”, “친구”, “이별”, “실수”처럼 구체 사건이나 관계가 직접 나오지 않으면 situationTagCodes는 비워도 된다.
    - 문장이 단순히 희망적이라고 해서 과한 긍정으로 보지 마라. avoidTagCodes는 부담스러운 표현이 명확할 때만 선택한다.
    
    [embeddingText 작성 기준]
    embeddingText는 인용구 원문을 추천 검색에 잘 걸리도록 설명한 짧은 문장이다.
    아래 정보를 자연스럽게 포함하라.
    
    - 인용구의 핵심 의미
    - 어울리는 감정
    - 어울리는 필요/역할
    - 문장의 분위기
    
    단, context나 situation이 명확하지 않으면 embeddingText에 억지로 장소/상황을 넣지 마라.
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
