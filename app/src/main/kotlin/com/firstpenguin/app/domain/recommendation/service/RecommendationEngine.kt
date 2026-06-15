package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.emotion.repository.TagRepository
import com.firstpenguin.app.domain.emotion.service.EmotionService
import com.firstpenguin.app.domain.recommendation.dto.RecommendationRequest
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.RecommendationResult
import com.firstpenguin.app.domain.recommendation.repository.RecommendationCandidateProvider
import com.firstpenguin.app.domain.recommendation.repository.RecommendationTagRarityRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service

private const val RECOMMENDATION_RESULT_QUOTE_COUNT = 10

@Service
class RecommendationEngine(
    private val emotionService: EmotionService,
    private val tagRepository: TagRepository,
    private val userInputAnalysisService: UserInputAnalysisService,
    private val effectiveTagBuilder: EffectiveTagBuilder,
    private val candidateProvider: RecommendationCandidateProvider,
    private val tagRarityRepository: RecommendationTagRarityRepository,
    private val resultComposer: RecommendationResultComposer,
) {
    fun recommend(
        userId: Long,
        request: RecommendationRequest,
    ): RecommendationResult {
        val input = buildInput(userId, request).withAnalysis()
        val effectiveTags = effectiveTagBuilder.build(input)
        val candidates = candidateProvider.findCandidates(effectiveTags)
        val result =
            resultComposer.compose(
                input = input,
                effectiveTags = effectiveTags,
                candidates = candidates,
                moodTagIdByCode = tagRepository.getActiveMoodTagIdByCode(),
                tagRarityWeights = tagRarityRepository.findMetadataTagRarityWeights(),
            ) ?: notEnoughQuotes()

        if (result.quotes.size < RECOMMENDATION_RESULT_QUOTE_COUNT) {
            notEnoughQuotes()
        }

        return result
    }

    private fun buildInput(
        userId: Long,
        request: RecommendationRequest,
    ): RecommendationInput {
        val selectedTagIds = request.emotionTagIds + listOfNotNull(request.needTagId)
        val (emotionTags, needTag) = emotionService.getEmotionTagsAndNeedTagByIds(selectedTagIds)

        return RecommendationInput(
            userId = userId,
            emotionRangeId = request.emotionRangeId,
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
