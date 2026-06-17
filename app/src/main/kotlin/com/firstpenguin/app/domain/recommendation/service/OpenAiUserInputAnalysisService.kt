package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.emotion.repository.TagRepository
import com.firstpenguin.app.domain.openai.dto.OpenAiTextResponse
import com.firstpenguin.app.domain.openai.service.OpenAiResponsesClient
import com.firstpenguin.app.domain.quotemetadata.dto.TagOption
import com.firstpenguin.app.domain.recommendation.model.RecommendationAiModelVersion
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.UserInputAnalysis
import com.firstpenguin.app.global.enums.TagType
import org.springframework.stereotype.Service

private typealias TagOptionGroups = Map<TagType, List<TagOption>>

@Service
class OpenAiUserInputAnalysisService(
    private val tagRepository: TagRepository,
    private val requestBuilder: UserInputParseRequestBuilder,
    private val outputParser: UserInputParseOutputParser,
    private val openAiResponsesClient: OpenAiResponsesClient,
) : UserInputAnalysisService {
    override fun analyze(input: RecommendationInput): UserInputAnalysis? =
        when {
            !input.hasText() -> {
                null
            }

            else -> {
                runCatching { analyzeOrThrow(input) }.getOrNull()
            }
        }

    private fun analyzeOrThrow(input: RecommendationInput): UserInputAnalysis {
        val tagGroups = findTagGroups(input)
        val request = requestBuilder.build(input, tagGroups)
        val response =
            measureRecommendationElapsed {
                openAiResponsesClient.createTextResponse(request)
            }

        return outputParser
            .parse(response.value.outputText, input, tagGroups)
            .withOpenAiUsage(request.model, response.value, response.elapsedMs)
    }

    private fun findTagGroups(input: RecommendationInput): TagOptionGroups =
        tagRepository
            .getActiveTagsByType()
            .filterKeys(USER_INPUT_PARSE_TAG_TYPES::contains)
            .onlyInputEmotionRange(input)

    private fun RecommendationInput.hasText(): Boolean = feelingText.hasValue() || diaryText.hasValue()

    private fun String?.normalizedText(): String? = this?.trim()?.takeIf { text -> text.isNotEmpty() }

    private fun String?.hasValue(): Boolean = normalizedText() != null

    private fun TagOptionGroups.onlyInputEmotionRange(input: RecommendationInput): TagOptionGroups =
        mapValues { (type, options) ->
            if (type != TagType.EMOTION) return@mapValues options

            options.filter { option -> option.emotionRangeId == input.emotionRangeId }
        }

    private fun UserInputAnalysis.withOpenAiUsage(
        model: String,
        response: OpenAiTextResponse,
        llmElapsedMs: Long?,
    ): UserInputAnalysis =
        copy(
            llmModel = model,
            llmModelVersion = RecommendationAiModelVersion.fromModel(model)?.version,
            inputTokens = response.inputTokens,
            cachedTokens = response.cachedTokens,
            outputTokens = response.outputTokens,
            llmElapsedMs = llmElapsedMs,
        )
}
