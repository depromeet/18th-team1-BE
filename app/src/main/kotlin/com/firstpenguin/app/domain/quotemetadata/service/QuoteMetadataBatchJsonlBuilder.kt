package com.firstpenguin.app.domain.quotemetadata.service

import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchModelVersion
import com.firstpenguin.app.domain.quotemetadata.dto.TagOption
import com.firstpenguin.app.global.enums.TagType
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

private val QUOTE_METADATA_PROMPT_GUIDE =
    """
너는 감정 기반 문장 추천 서비스의 인용구 태그 분석기다.

너의 역할은 책의 인용구/문장을 분석하여, 제공된 허용 tagCode 안에서만 관련 태그를 선택하는 것이다.

이 작업의 목적은 인용구를 추천 시스템에서 검색/랭킹할 수 있도록 메타데이터화하는 것이다.

중요 규칙:
1. 새로운 태그를 만들지 마라.
2. 반드시 제공된 tagCode 중에서만 선택하라.
3. label과 description은 태그 선택 기준이다. 특히 description의 선택 조건과 제외 조건을 우선한다.
4. roleTagCode는 반드시 1개만 선택하라.
5. emotionTagCodes와 needTagCodes는 각각 1~2개 선택하라.
6. moodTagCodes는 가장 알맞은 1개만 선택하라.
7. contextTagCodes와 situationTagCodes는 대부분 빈 배열이어야 한다.
8. contextTagCodes와 situationTagCodes는 원문에 명확한 직접 근거가 있을 때만 예외적으로 선택한다.
9. contextTagCodes는 실제 장소, 날씨, 시간, 활동, 장면이 드러날 때만 선택하라.
10. situationTagCodes는 실제 삶의 문제, 사건, 관계, 주제가 드러날 때만 선택하라.
11. 자연물, 날씨, 장소, 길, 계절이 마음 상태를 꾸미는 은유라면 contextTagCodes를 비워라.
12. 개수를 채우기 위해 약한 태그를 선택하지 마라.
13. 같은 배열 안에서 동일한 tagCode를 중복 선택하지 마라.
14. emotionTagCodes, needTagCodes, moodTagCodes는 가장 강한 태그 1개를 먼저 고르고, 두 번째는 명확할 때만 추가하라.
15. contextTagCodes와 situationTagCodes는 관련성이 약하면 빈 배열을 반환하라.
16. 출력은 반드시 JSON schema를 따르고 JSON 외의 설명 문장은 출력하지 마라.

[태그 구분 기준]
- emotionTagCodes는 이 인용구를 추천하기 좋은 사용자의 현재 감정 상태다.
- 인용구 속 감정이나 읽고 난 뒤 생길 기쁨, 안도, 활기, 희망은 emotionTagCodes가 아니다.
- 사용자가 어떤 감정을 가진 상태에서 이 인용구를 받으면 좋을지 먼저 판단하라.
- 변화 앞의 막막함, 망설임, 불안, 성찰처럼 추천 전의 마음을 고르고, 변화 뒤의 밝은 결과 감정은 고르지 마라.
- needTagCodes는 이 인용구가 사용자에게 해주는 일이다.
- moodTagCodes는 인용구의 말투와 분위기다. 사용자의 마음이나 필요와 섞지 마라.
- 변화나 회복의 효과는 emotionTagCodes보다 roleTagCode와 needTagCodes에 먼저 반영하라.

[roleTagCode 세부 기준]
- 감정을 알아주거나 공감하는 의미가 가장 강하면 ROLE_EMPATHY를 선택한다.
- 관점 전환, 본질, 성찰, 마음 정리의 의미가 가장 강하면 ROLE_PERSPECTIVE를 선택한다.
- 용기, 재시작, 변화, 성장, 행동으로 나아감의 의미가 가장 강하면 ROLE_RECOVERY를 선택한다.

[embeddingText 작성 기준]
- embeddingText는 사용자가 검색창에 입력할 법한 사용자 의도 문장이다.
- 추천받을 사용자의 현재 마음과 원하는 도움만 적어라.
- 인용구를 읽고 난 뒤의 결과를 단정하지 마라.
- 책, 인용구, 원문, 독자, 문장 자체를 설명하지 마라.
- 원문 내용을 길게 풀어쓰거나 그대로 반복하지 마라.
- 마지막은 “~필요하다”, “~원한다”, “~싶다”, “~고 싶다”처럼 사용자 요청이 드러나게 끝내라.
- tagCode를 쓰지 마라.
- label을 단순 나열하지 마라.
- context나 situation이 명확하지 않으면 장소/상황을 넣지 마라.
- 다음 단어를 절대 쓰지 마라: 인용구, 문장, 문구, 독자, 메시지, 분류, 해당, 태그, 해석, 한 줄.
- “~에게 어울린다”, “~에 적합하다”, “~를 제공한다”처럼 설명 보고서 말투를 쓰지 마라.
- 좋은 형태: 삶의 방향이 흐릿할 때 마음을 정리하고 새로운 시각이 필요하다
- 나쁜 형태: 새로운 시각을 얻고 다시 시작할 힘이 생긴다
- 35~75자 사이의 한국어 한 문장으로 작성하라.
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
                            "model" to QuoteBatchModelVersion.QUOTE_METADATA_V1.model,
                            "reasoning" to
                                mapOf(
                                    "effort" to "low",
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
}

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
    return "- label: ${option.label} | tagCode: ${option.code} | description: $description"
}
