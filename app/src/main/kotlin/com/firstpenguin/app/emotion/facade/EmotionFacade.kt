package com.firstpenguin.app.emotion.facade

import com.firstpenguin.app.emotion.dto.TagResponse
import com.firstpenguin.app.emotion.dto.TagSelectRequest
import com.firstpenguin.app.emotion.dto.TagSelectResponse
import com.firstpenguin.app.emotion.service.EmotionService
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