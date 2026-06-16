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
        val tagGroups = findTagGroups()
        val request = requestBuilder.build(input, tagGroups)
        val response =
            measureRecommendationElapsed {
                openAiResponsesClient.createTextResponse(request)
            }

        return outputParser
            .parse(response.value.outputText, input, tagGroups)
            .withOpenAiUsage(request.model, response.value, response.elapsedMs)
    }

    private fun findTagGroups(): Map<TagType, List<TagOption>> =
        tagRepository
            .getActiveTagsByType()
            .onlyUserInputParseTagGroups()

    private fun RecommendationInput.hasText(): Boolean = feelingText.hasValue() || diaryText.hasValue()

    private fun String?.normalizedText(): String? = this?.trim()?.takeIf { text -> text.isNotEmpty() }

    private fun String?.hasValue(): Boolean = normalizedText() != null

    private fun Map<TagType, List<TagOption>>.onlyUserInputParseTagGroups(): Map<TagType, List<TagOption>> =
        filterKeys { type -> type in USER_INPUT_PARSE_TAG_TYPES }

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
