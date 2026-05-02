package com.firstpenguin.app.domain.emotion.useCase

import com.firstpenguin.app.domain.emotion.dto.TagResponse
import com.firstpenguin.app.domain.emotion.dto.TagSelectRequest
import com.firstpenguin.app.domain.emotion.dto.TagSelectResponse
import com.firstpenguin.app.domain.emotion.service.EmotionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EmotionUseCase(
    private val emotionService: EmotionService,
) {
    @Transactional(readOnly = true)
    fun getEmotionTags(value: Int): TagResponse = emotionService.getEmotionTags(value)

    @Transactional(readOnly = true)
    fun getToneTags(): TagResponse = emotionService.getToneTags()

    @Transactional(readOnly = true)
    fun selectEmotionTags(request: TagSelectRequest): TagSelectResponse {
        // 문장 추천
        // 감정 온도 카테고리에 맞지 않은 태그선택 막기
        return emotionService.selectEmotionTags(request.emotionTagIds, request.toneTagIds)
    }
}
