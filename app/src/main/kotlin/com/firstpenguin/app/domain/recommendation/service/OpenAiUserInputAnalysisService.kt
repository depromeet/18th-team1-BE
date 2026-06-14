package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.emotion.repository.TagRepository
import com.firstpenguin.app.domain.openai.service.OpenAiResponsesClient
import com.firstpenguin.app.domain.quotemetadata.dto.TagOption
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
    override fun analyze(input: RecommendationInput): UserInputAnalysis? {
        if (!input.hasText()) return null

        return runCatching { analyzeOrThrow(input) }.getOrNull()
    }

    private fun analyzeOrThrow(input: RecommendationInput): UserInputAnalysis {
        val tagGroups = tagRepository.getActiveTagsByType().onlyUserInputParseTagGroups()
        val request = requestBuilder.build(input, tagGroups)
        val outputText = openAiResponsesClient.createTextResponse(request)

        return outputParser.parse(outputText, input, tagGroups)
    }

    private fun RecommendationInput.hasText(): Boolean = feelingText.hasValue() || diaryText.hasValue()

    private fun String?.normalizedText(): String? = this?.trim()?.takeIf { text -> text.isNotEmpty() }

    private fun String?.hasValue(): Boolean = normalizedText() != null

    private fun Map<TagType, List<TagOption>>.onlyUserInputParseTagGroups(): Map<TagType, List<TagOption>> =
        filterKeys { type -> type in USER_INPUT_PARSE_TAG_TYPES }
}
