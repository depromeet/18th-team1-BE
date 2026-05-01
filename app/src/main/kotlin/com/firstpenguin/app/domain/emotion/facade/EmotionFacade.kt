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
    fun getEmotionTags(value: Int): TagResponse = emotionService.getEmotionTags(value)

    fun getToneTags(): TagResponse = emotionService.getToneTags()

    fun selectEmotionTags(request: TagSelectRequest): TagSelectResponse {
        // 문장 추천
        // 감정 온도 카테고리에 맞지 않은 태그선택 막기
        return emotionService.selectEmotionTags(request.emotionTagIds, request.toneTagIds)
    }
}
