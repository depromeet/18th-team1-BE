package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.emotion.service.EmotionService
import com.firstpenguin.app.domain.recommendation.dto.RecommendationRequest
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.RecommendationResult
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service

private const val RECOMMENDATION_RESULT_QUOTE_COUNT = 10

@Service
class RecommendationEngine(
    private val emotionService: EmotionService,
    private val userInputAnalysisService: UserInputAnalysisService,
    private val effectiveTagBuilder: EffectiveTagBuilder,
    private val prefetcher: RecommendationEnginePrefetcher,
    private val resultComposer: RecommendationResultComposer,
) {
    fun recommend(
        userId: Long,
        request: RecommendationRequest,
    ): RecommendationResult {
        val input = buildInput(userId, request)
        val prefetch = prefetcher.start(effectiveTagBuilder.build(input))
        val analyzedInput = input.withAnalysis()
        val effectiveTags = effectiveTagBuilder.build(analyzedInput)
        val result =
            resultComposer.compose(
                input = analyzedInput,
                effectiveTags = effectiveTags,
                candidates = prefetch.candidates(),
                moodTagIdByCode = prefetch.moodTagIdByCode(),
                tagRarityWeights = prefetch.tagRarityWeights(),
            ) ?: notEnoughQuotes()

        return result
            .takeIf { recommendationResult -> recommendationResult.quotes.size >= RECOMMENDATION_RESULT_QUOTE_COUNT }
            ?: notEnoughQuotes()
    }

    private fun buildInput(
        userId: Long,
        request: RecommendationRequest,
    ): RecommendationInput {
        val selectedTagIds = request.emotionTagIds + listOfNotNull(request.needTagId)
        val emotionRange = emotionService.getEmotionRangeByValue(request.emotionValue)
        val (emotionTags, needTag) = emotionService.getEmotionTagsAndNeedTagByIds(selectedTagIds)

        return RecommendationInput(
            userId = userId,
            emotionValue = request.emotionValue,
            emotionRangeId = emotionRange.id,
            emotionTags = emotionTags,
            needTag = needTag,
            feelingText = request.feelingText.normalizedText(),
            diaryText = request.diaryText.normalizedText(),
            analysis = null,
        )
    }

    private fun RecommendationInput.withAnalysis(): RecommendationInput =
        copy(
            analysis = userInputAnalysisService.analyze(this),
        )
}

private fun String?.normalizedText(): String? = this?.trim()?.takeIf { text -> text.isNotEmpty() }

private fun notEnoughQuotes(): Nothing = throw CustomException(ErrorCode.NOT_ENOUGH_QUOTES)
