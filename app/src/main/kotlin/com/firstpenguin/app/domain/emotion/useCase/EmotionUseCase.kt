package com.firstpenguin.app.domain.emotion.useCase

import com.firstpenguin.app.domain.emotion.dto.TagDto
import com.firstpenguin.app.domain.emotion.dto.TagResponse
import com.firstpenguin.app.domain.emotion.service.EmotionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EmotionUseCase(
    private val emotionService: EmotionService,
) {
    @Transactional(readOnly = true)
    fun getEmotionTags(value: Int): TagResponse {
        val emotionTags = emotionService.getEmotionTags(value)

        return TagResponse(
            tags =
                emotionTags.map(TagDto::from),
        )
    }

    @Transactional(readOnly = true)
    fun getNeedTags(): TagResponse {
        val needTags = emotionService.getNeedTags()

        return TagResponse(
            tags =
                needTags.map(TagDto::from),
        )
    }
}
