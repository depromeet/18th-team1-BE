package com.firstpenguin.app.domain.emotion.facade

import com.firstpenguin.app.domain.emotion.dto.TagResponse
import com.firstpenguin.app.domain.emotion.dto.TagSelectRequest
import com.firstpenguin.app.domain.emotion.dto.TagSelectResponse
import com.firstpenguin.app.domain.emotion.service.EmotionService
import org.springframework.stereotype.Service

@Service
class EmotionFacade(
    private val emotionService: EmotionService,
) {
    fun getEmotionTags(value: Int): TagResponse {
        return emotionService.getEmotionTags(value)
    }

    fun selectEmotionTags(request: TagSelectRequest): TagSelectResponse {
        return emotionService.selectEmotionTags(request.emotionTagIds, request.toneTagIds)
    }
}