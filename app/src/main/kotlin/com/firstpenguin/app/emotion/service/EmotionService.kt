package com.firstpenguin.app.emotion.service

import com.firstpenguin.app.emotion.dto.TagDto
import com.firstpenguin.app.emotion.dto.TagResponse
import com.firstpenguin.app.emotion.dto.TagSelectResponse
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

        val tagList = tagRepository.getTagListByEmotionRangeId(emotionRange.id)

        return TagResponse(
            tagList = tagList.map {
                TagDto(
                    id = it.id,
                    label = it.label,
                    type = it.type
                )
            }
        )
    }

    @Transactional(readOnly = true)
    fun selectEmotionTags(emotionTagIds: List<Long>, toneTagIds: List<Long>): TagSelectResponse {
        val emotionTags = tagRepository.getEmotionTags(emotionTagIds)
        val toneTags = tagRepository.getToneTags(toneTagIds)

        if (emotionTags.size != emotionTagIds.distinct().size) {
            throw CustomException(ErrorCode.INVALID_EMOTION_TAG)
        }

        if (toneTags.size != toneTagIds.distinct().size) {
            throw CustomException(ErrorCode.INVALID_TONE_TAG)
        }

        return TagSelectResponse(
            emotionTags = emotionTags.map {
                TagDto(
                    id = it.id,
                    label = it.label,
                    type = it.type
                )
            },
            toneTags = toneTags.map {
                TagDto(
                    id = it.id,
                    label = it.label,
                    type = it.type
                )
            }
        )
    }
}
