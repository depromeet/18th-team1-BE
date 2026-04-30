package com.firstpenguin.app.emotion.service

import com.firstpenguin.app.emotion.dto.TagDto
import com.firstpenguin.app.emotion.dto.TagResponse
import com.firstpenguin.app.emotion.repository.EmotionRangeRepository
import com.firstpenguin.app.emotion.repository.TagRepository
import com.firstpenguin.app.global.exception.ErrorCode
import com.firstpenguin.app.global.exception.CustomException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EmotionService(
    private val emotionRangeRepository: EmotionRangeRepository,
    private val tagRepository: TagRepository
) {

    @Transactional(readOnly = true)
    fun getEmotionTags(value: Int): TagResponse {
        val emotionRange = emotionRangeRepository.getEmotionRange(value)
            ?: throw CustomException(ErrorCode.NOT_FOUND_EMOTION_RANGE)

        val tagList = tagRepository.getTagList(emotionRange.id)

        return TagResponse(
            tagList = tagList.map {
                TagDto(
                    id = it.id,
                    label = it.label
                )
            }
        )
    }
}
