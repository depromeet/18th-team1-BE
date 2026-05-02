package com.firstpenguin.app.domain.emotion.service

import com.firstpenguin.app.domain.emotion.dto.TagDto
import com.firstpenguin.app.domain.emotion.dto.TagResponse
import com.firstpenguin.app.domain.emotion.dto.TagSelectResponse
import com.firstpenguin.app.domain.emotion.model.Tag
import com.firstpenguin.app.domain.emotion.repository.EmotionRangeRepository
import com.firstpenguin.app.domain.emotion.repository.TagRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service
import kotlin.collections.map

@Service
class EmotionService(
    private val emotionRangeRepository: EmotionRangeRepository,
    private val tagRepository: TagRepository,
) {
    fun getEmotionTags(value: Int): TagResponse {
        val emotionRange =
            emotionRangeRepository.getEmotionRange(value)
                ?: throw CustomException(ErrorCode.NOT_FOUND_EMOTION_RANGE)

        val emotionTags = tagRepository.getEmotionTagsByEmotionRangeId(emotionRange.id)

        return TagResponse(
            tags =
                emotionTags.map {
                    TagDto(
                        id = it.id,
                        label = it.label,
                        type = it.type,
                    )
                },
        )
    }

    fun getToneTags(): TagResponse {
        val toneTags = tagRepository.getToneTags()

        return TagResponse(
            tags =
                toneTags.map {
                    TagDto(
                        id = it.id,
                        label = it.label,
                        type = it.type,
                    )
                },
        )
    }

    fun selectEmotionTags(
        emotionTagIds: List<Long>,
        toneTagIds: List<Long>,
    ): TagSelectResponse {
        val emotionTags = tagRepository.getEmotionTagsByTagIdsIn(emotionTagIds)
        val toneTags = tagRepository.getToneTagsByTagIdsIn(toneTagIds)

        validateEmotionTags(emotionTags, emotionTagIds)
        validateToneTags(toneTags, toneTagIds)
        validateSameEmotionRange(emotionTags)

        return TagSelectResponse(
            emotionTags =
                emotionTags.map {
                    TagDto(
                        id = it.id,
                        label = it.label,
                        type = it.type,
                    )
                },
            toneTags =
                toneTags.map {
                    TagDto(
                        id = it.id,
                        label = it.label,
                        type = it.type,
                    )
                },
        )
    }

    private fun validateEmotionTags(
        emotionTags: List<Tag>,
        emotionTagIds: List<Long>,
    ) {
        if (emotionTags.size != emotionTagIds.distinct().size) {
            throw CustomException(ErrorCode.INVALID_EMOTION_TAG)
        }
    }

    private fun validateToneTags(
        toneTags: List<Tag>,
        toneTagIds: List<Long>,
    ) {
        if (toneTags.size != toneTagIds.distinct().size) {
            throw CustomException(ErrorCode.INVALID_TONE_TAG)
        }
    }

    private fun validateSameEmotionRange(emotionTags: List<Tag>) {
        val rangeIds = emotionTags.mapNotNull { it.emotionRangeId }.toSet()
        if (rangeIds.size != 1) {
            throw CustomException(ErrorCode.INVALID_EMOTION_TAG_RANGE)
        }
    }
}
