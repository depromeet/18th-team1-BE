package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.emotion.service.EmotionService
import com.firstpenguin.app.domain.recommendation.dto.RecommendationRequest
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service

private const val MAX_EMOTION_TAG_COUNT = 5

@Service
class RecommendationRequestValidator(
    private val emotionService: EmotionService,
) {
    fun validate(request: RecommendationRequest) {
        validateEmotionTagCount(request.emotionTagIds)
        validateNeedInput(request)
        emotionService.validateTags(
            emotionRangeId = request.emotionRangeId,
            emotionTagIds = request.emotionTagIds,
            needTagId = request.needTagId,
        )
    }

    private fun validateEmotionTagCount(emotionTagIds: List<Long>) {
        if (emotionTagIds.isEmpty() || emotionTagIds.size > MAX_EMOTION_TAG_COUNT) {
            throw CustomException(ErrorCode.INVALID_EMOTION_TAG)
        }
    }

    private fun validateNeedInput(request: RecommendationRequest) {
        val hasNeedTag = request.needTagId != null
        val hasFeelingText = request.feelingText.hasText()

        if (hasNeedTag == hasFeelingText) {
            throw CustomException(ErrorCode.INVALID_RECOMMENDATION_NEED_INPUT)
        }
    }
}

private fun String?.hasText(): Boolean = !isNullOrBlank()
