package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.emotion.repository.TagRepository
import com.firstpenguin.app.domain.openai.dto.OpenAiTextResponse
import com.firstpenguin.app.domain.openai.service.OpenAiResponsesClient
import com.firstpenguin.app.domain.quotemetadata.dto.TagOption
import com.firstpenguin.app.domain.recommendation.model.RecommendationAiModelVersion
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.UserInputAnalysis
import com.firstpenguin.app.global.enums.TagType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

private typealias TagOptionGroups = Map<TagType, List<TagOption>>

@Service
class OpenAiUserInputAnalysisService(
    private val tagRepository: TagRepository,
    private val canonicalRequestBuilder: UserCanonicalIntentRequestBuilder,
    private val canonicalOutputParser: UserCanonicalIntentOutputParser,
    private val tagRequestBuilder: UserInputParseRequestBuilder,
    private val tagOutputParser: UserInputParseOutputParser,
    private val openAiResponsesClient: OpenAiResponsesClient,
    @Qualifier(RECOMMENDATION_ANALYSIS_EXECUTOR_NAME) private val analysisExecutor: Executor,
) : UserInputAnalysisService {
    override fun start(input: RecommendationInput): UserInputAnalysisTask {
        if (!input.hasText()) return UserInputAnalysisTask.completed(null)

        val tagGroups = findTagGroups(input)
        val canonicalAnalysis = async { analyzeCanonicalIntent(input) }
        val tagAnalysis = async { analyzeTags(input, tagGroups) }
        val analysis =
            canonicalAnalysis.thenCombine(tagAnalysis) { canonical, tags ->
                combineAnalysis(canonical, tags)
            }

        return UserInputAnalysisTask(canonicalAnalysis, analysis)
    }

    private fun analyzeCanonicalIntent(input: RecommendationInput): UserInputAnalysis {
        val request = canonicalRequestBuilder.build(input)
        val response =
            measureRecommendationElapsed {
                openAiResponsesClient.createTextResponse(request)
            }

        return UserInputAnalysis(
            canonicalIntent = canonicalOutputParser.parse(response.value.outputText),
            tagCandidates = emptyList(),
        ).withOpenAiUsage(request.model, response.value, response.elapsedMs)
    }

    private fun analyzeTags(
        input: RecommendationInput,
        tagGroups: TagOptionGroups,
    ): UserInputAnalysis {
        val request = tagRequestBuilder.build(input, tagGroups)
        val response =
            measureRecommendationElapsed {
                openAiResponsesClient.createTextResponse(request)
            }

        return tagOutputParser
            .parse(response.value.outputText, input, tagGroups)
            .withOpenAiUsage(request.model, response.value, response.elapsedMs)
    }

    private fun findTagGroups(input: RecommendationInput): TagOptionGroups =
        tagRepository
            .getActiveTagsByType()
            .filterKeys(USER_INPUT_PARSE_TAG_TYPES::contains)
            .withoutNeedIfSelected(input)

    private fun async(block: () -> UserInputAnalysis): CompletableFuture<UserInputAnalysis?> =
        CompletableFuture.supplyAsync(
            { runCatching(block).getOrNull() },
            analysisExecutor,
        )

    private fun RecommendationInput.hasText(): Boolean = feelingText.hasValue() || diaryText.hasValue()

    private fun String?.normalizedText(): String? = this?.trim()?.takeIf { text -> text.isNotEmpty() }

    private fun String?.hasValue(): Boolean = normalizedText() != null

    private fun TagOptionGroups.withoutNeedIfSelected(input: RecommendationInput): TagOptionGroups {
        if (input.needTag == null) return this

        return filterKeys { type -> type != TagType.NEED }
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

private fun combineAnalysis(
    canonicalAnalysis: UserInputAnalysis?,
    tagAnalysis: UserInputAnalysis?,
): UserInputAnalysis? {
    if (canonicalAnalysis == null && tagAnalysis == null) return null

    return UserInputAnalysis(
        canonicalIntent = canonicalAnalysis?.canonicalIntent,
        tagCandidates = tagAnalysis?.tagCandidates.orEmpty(),
        llmModel = canonicalAnalysis?.llmModel ?: tagAnalysis?.llmModel,
        llmModelVersion = canonicalAnalysis?.llmModelVersion ?: tagAnalysis?.llmModelVersion,
        inputTokens = sum(canonicalAnalysis?.inputTokens, tagAnalysis?.inputTokens),
        cachedTokens = sum(canonicalAnalysis?.cachedTokens, tagAnalysis?.cachedTokens),
        outputTokens = sum(canonicalAnalysis?.outputTokens, tagAnalysis?.outputTokens),
        llmElapsedMs = max(canonicalAnalysis?.llmElapsedMs, tagAnalysis?.llmElapsedMs),
    )
}

private fun sum(
    first: Long?,
    second: Long?,
): Long? {
    if (first == null && second == null) return null

    return first.orZero() + second.orZero()
}

private fun max(
    first: Long?,
    second: Long?,
): Long? {
    if (first == null && second == null) return null

    return maxOf(first.orZero(), second.orZero())
}

private fun Long?.orZero(): Long = this ?: 0L
