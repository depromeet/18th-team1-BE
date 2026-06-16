package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.emotion.repository.TagRepository
import com.firstpenguin.app.domain.openai.service.OpenAiResponsesClient
import com.firstpenguin.app.domain.quotemetadata.dto.TagOption
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.UserInputAnalysis
import com.firstpenguin.app.global.enums.TagType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private const val MIN_DIARY_TEXT_LENGTH_FOR_ANALYSIS = 20

@Service
class OpenAiUserInputAnalysisService(
    private val tagRepository: TagRepository,
    private val requestBuilder: UserInputParseRequestBuilder,
    private val outputParser: UserInputParseOutputParser,
    private val openAiResponsesClient: OpenAiResponsesClient,
) : UserInputAnalysisService {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun analyze(input: RecommendationInput): UserInputAnalysis? =
        when {
            input.shouldSkipAnalysis() -> {
                log.measureRecommendationStep("userInputAnalysis.skip", { input.skipLogDetail() }) {}
                null
            }

            !input.hasText() -> {
                null
            }

            else -> {
                runCatching {
                    log.measureRecommendationStep("userInputAnalysis.total", { "userId=${input.userId}" }) {
                        analyzeOrThrow(input)
                    }
                }.getOrNull()
            }
        }

    private fun analyzeOrThrow(input: RecommendationInput): UserInputAnalysis {
        val tagGroups = findMeasuredTagGroups(input.userId)
        val request =
            log.measureRecommendationStep("userInputAnalysis.buildRequest", { "userId=${input.userId}" }) {
                requestBuilder.build(input, tagGroups)
            }
        val outputText =
            log.measureRecommendationStep("userInputAnalysis.openAi", { "userId=${input.userId}" }) {
                openAiResponsesClient.createTextResponse(request)
            }

        return log.measureRecommendationStep("userInputAnalysis.parse", { "userId=${input.userId}" }) {
            outputParser.parse(outputText, input, tagGroups)
        }
    }

    private fun findMeasuredTagGroups(userId: Long): Map<TagType, List<TagOption>> {
        var typeCount = 0
        lateinit var tagGroups: Map<TagType, List<TagOption>>

        log.measureRecommendationStep("userInputAnalysis.tagOptions", { "userId=$userId types=$typeCount" }) {
            tagGroups = tagRepository.getActiveTagsByType().onlyUserInputParseTagGroups()
            typeCount = tagGroups.size
        }

        return tagGroups
    }

    private fun RecommendationInput.hasText(): Boolean = feelingText.hasValue() || diaryText.hasValue()

    private fun String?.normalizedText(): String? = this?.trim()?.takeIf { text -> text.isNotEmpty() }

    private fun String?.hasValue(): Boolean = normalizedText() != null

    private fun Map<TagType, List<TagOption>>.onlyUserInputParseTagGroups(): Map<TagType, List<TagOption>> =
        filterKeys { type -> type in USER_INPUT_PARSE_TAG_TYPES }
}

private fun RecommendationInput.shouldSkipAnalysis(): Boolean = needTag != null && diaryText.isBlankOrShort()

private fun RecommendationInput.skipLogDetail(): String =
    "userId=$userId reason=selectedNeedTagAndShortDiaryText diaryTextLength=${diaryText.normalizedLength()}"

private fun String?.isBlankOrShort(): Boolean = normalizedLength() < MIN_DIARY_TEXT_LENGTH_FOR_ANALYSIS

private fun String?.normalizedLength(): Int = this?.trim()?.length ?: 0
