package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.openai.dto.OpenAiResponsesRequest
import com.firstpenguin.app.domain.openai.dto.OpenAiResponsesTextRequest
import com.firstpenguin.app.domain.recommendation.model.RecommendationAiModelVersion
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

private const val OPENAI_REASONING_EFFORT = "low"
private const val OPENAI_TEXT_VERBOSITY = "low"
private const val OPENAI_PROMPT_CACHE_RETENTION = "24h"
private const val CANONICAL_INTENT_MAX_OUTPUT_TOKENS = 160
private const val CANONICAL_INTENT_PROMPT_CACHE_KEY_PREFIX = "user-canonical-intent-v1"

private val USER_CANONICAL_INTENT_PROMPT_GUIDE =
    """
    너는 감정 기반 문장 추천 서비스의 사용자 입력 요약기다.

    너의 역할은 사용자의 free text와 diary text를 분석하여 추천 엔진이 사용할 canonicalIntent만 반환하는 것이다.

    중요 규칙:
    1. feelingText는 사용자가 원하는 추천 의도를 나타내는 가장 중요한 텍스트다.
    2. diaryText는 배경 정보로만 사용하라.
    3. feelingText와 diaryText가 충돌하면 feelingText를 우선하라.
    4. 한국어 한 문장으로 작성하라.
    5. 사용자의 현재 마음과 원하는 도움을 요약하라.
    6. quote, 문장, 추천 결과를 설명하지 마라.
    7. tagCode를 언급하거나 단순 나열하지 마라.
    8. 35~90자 사이를 권장한다.
    9. 출력은 반드시 JSON schema를 따르고 JSON 외의 설명 문장은 출력하지 마라.

    좋은 형태: 실패한 뒤 불안하고 위축된 마음을 다독이며 다시 관점을 정리하고 싶다
    나쁜 형태: 이 사용자에게 위로와 관점 전환 태그가 적합하다
    """.trimIndent()

@Component
class UserCanonicalIntentRequestBuilder(
    private val jsonMapper: JsonMapper,
) {
    fun build(input: RecommendationInput): OpenAiResponsesRequest =
        OpenAiResponsesRequest(
            model = USER_CANONICAL_INTENT_MODEL.model,
            reasoning = mapOf("effort" to OPENAI_REASONING_EFFORT),
            input = buildPrompt(input),
            text =
                OpenAiResponsesTextRequest(
                    format = userCanonicalIntentSchema(),
                    verbosity = OPENAI_TEXT_VERBOSITY,
                ),
            promptCacheKey = USER_CANONICAL_INTENT_MODEL.canonicalPromptCacheKey,
            promptCacheRetention = OPENAI_PROMPT_CACHE_RETENTION,
            maxOutputTokens = CANONICAL_INTENT_MAX_OUTPUT_TOKENS,
        )

    private fun buildPrompt(input: RecommendationInput): String =
        listOf(
            USER_CANONICAL_INTENT_PROMPT_GUIDE,
            userInputPayload(input),
        ).joinToString("\n\n")

    private fun userInputPayload(input: RecommendationInput): String =
        """
        [분석 대상 사용자 입력]
        ${jsonMapper.writeValueAsString(input.toPayload())}
        """.trimIndent()

    private fun RecommendationInput.toPayload(): Map<String, String?> =
        mapOf(
            "feelingText" to feelingText.normalizedText(),
            "diaryText" to diaryText.normalizedText(),
        )

    private fun String?.normalizedText(): String? = this?.trim()?.takeIf { text -> text.isNotEmpty() }
}

private val RecommendationAiModelVersion.canonicalPromptCacheKey: String
    get() = "$CANONICAL_INTENT_PROMPT_CACHE_KEY_PREFIX-$model"

private val USER_CANONICAL_INTENT_MODEL = RecommendationAiModelVersion.USER_INPUT_ANALYSIS_V1
