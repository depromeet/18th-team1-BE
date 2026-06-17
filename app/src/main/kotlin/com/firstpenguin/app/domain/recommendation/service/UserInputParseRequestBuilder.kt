package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.openai.dto.OpenAiResponsesRequest
import com.firstpenguin.app.domain.openai.dto.OpenAiResponsesTextRequest
import com.firstpenguin.app.domain.quotemetadata.dto.TagOption
import com.firstpenguin.app.domain.recommendation.model.RecommendationAiModelVersion
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.global.enums.TagType
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

private const val OPENAI_REASONING_EFFORT = "low"
private const val OPENAI_TEXT_VERBOSITY = "low"
private const val OPENAI_PROMPT_CACHE_RETENTION = "24h"
private const val USER_INPUT_ANALYSIS_PROMPT_CACHE_KEY_PREFIX = "user-input-analysis-v1"

private val USER_INPUT_PARSE_PROMPT_GUIDE =
    """
너는 감정 기반 문장 추천 서비스의 사용자 입력 분석기다.

너의 역할은 사용자의 free text와 diary text를 분석하여 추천 엔진이 사용할 canonicalIntent,
타입별 tag candidate를 반환하는 것이다.

중요 규칙:
1. 반드시 제공된 tagCode 중에서만 선택하라.
2. feelingText는 사용자가 원하는 추천 의도를 나타내는 가장 중요한 텍스트다.
3. diaryText는 배경 정보로만 사용하라.
4. feelingText와 diaryText가 충돌하면 feelingText를 우선하라.
5. context/situation tag를 중심으로 추출하라.
6. emotion/need tag는 명확한 근거가 있을 때만 추출하라.
7. 개수를 채우기 위해 약한 태그를 선택하지 마라.
8. emotion tag는 사용자가 고른 감정 태그를 보조하는 후보이므로 PRIMARY priority를 사용하지 마라.
9. need tag는 원하는 도움이나 기대가 명확히 드러날 때만 1개까지 추출하라.
10. context tag는 실제 장소, 날씨, 시간, 활동, 장면이 직접 드러날 때만 선택하라.
11. situation tag는 실제 삶의 문제, 사건, 관계, 주제가 직접 드러날 때만 선택하라.
12. 은유적 표현만으로 context나 situation을 선택하지 마라.
13. 출력은 반드시 JSON schema를 따르고 JSON 외의 설명 문장은 출력하지 마라.

[canonicalIntent 작성 기준]
- 한국어 한 문장으로 작성하라.
- 사용자의 현재 마음과 원하는 도움을 요약하라.
- quote, 문장, 추천 결과를 설명하지 마라.
- tagCode를 단순 나열하지 마라.
- 35~90자 사이를 권장한다.
- 좋은 형태: 실패한 뒤 불안하고 위축된 마음을 다독이며 다시 관점을 정리하고 싶다
- 나쁜 형태: 이 사용자에게 위로와 관점 전환 태그가 적합하다
    """.trimIndent()

@Component
class UserInputParseRequestBuilder(
    private val jsonMapper: JsonMapper,
) {
    fun build(
        input: RecommendationInput,
        tagGroups: Map<TagType, List<TagOption>>,
    ): OpenAiResponsesRequest =
        OpenAiResponsesRequest(
            model = USER_INPUT_ANALYSIS_MODEL.model,
            reasoning = mapOf("effort" to OPENAI_REASONING_EFFORT),
            input = buildPrompt(input, tagGroups),
            text =
                OpenAiResponsesTextRequest(
                    format = userInputParseSchema(tagGroups),
                    verbosity = OPENAI_TEXT_VERBOSITY,
                ),
            promptCacheKey = USER_INPUT_ANALYSIS_MODEL.promptCacheKey,
            promptCacheRetention = OPENAI_PROMPT_CACHE_RETENTION,
        )

    private fun buildPrompt(
        input: RecommendationInput,
        tagGroups: Map<TagType, List<TagOption>>,
    ): String =
        listOf(
            USER_INPUT_PARSE_PROMPT_GUIDE,
            allowedTagsPayload(tagGroups),
            userInputPayload(input),
        ).joinToString("\n\n")

    private fun allowedTagsPayload(tagGroups: Map<TagType, List<TagOption>>): String =
        """
        [허용 태그 목록]
        ${jsonMapper.writeValueAsString(tagGroups.toAllowedTagsPayload())}
        """.trimIndent()

    private fun userInputPayload(input: RecommendationInput): String =
        """
        [분석 대상 사용자 입력]
        ${jsonMapper.writeValueAsString(input.toPayload())}
        """.trimIndent()

    private fun RecommendationInput.toPayload(): Map<String, Any?> =
        mapOf(
            "feelingText" to feelingText.normalizedText(),
            "diaryText" to diaryText.normalizedText(),
        )

    private fun String?.normalizedText(): String? = this?.trim()?.takeIf { text -> text.isNotEmpty() }

    private fun Map<TagType, List<TagOption>>.toAllowedTagsPayload(): Map<String, List<Map<String, String>>> =
        USER_INPUT_PARSE_TAG_TYPES.associate { type ->
            type.name to get(type).orEmpty().map { option -> option.toPayload() }
        }

    private fun TagOption.toPayload(): Map<String, String> =
        mapOf(
            "tagCode" to code,
            "description" to description.orEmpty(),
        )
}

private val RecommendationAiModelVersion.promptCacheKey: String
    get() = "$USER_INPUT_ANALYSIS_PROMPT_CACHE_KEY_PREFIX-$model"

private val USER_INPUT_ANALYSIS_MODEL = RecommendationAiModelVersion.USER_INPUT_ANALYSIS_V1
