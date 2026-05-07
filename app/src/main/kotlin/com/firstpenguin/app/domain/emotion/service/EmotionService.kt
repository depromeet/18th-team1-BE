package com.firstpenguin.app.domain.emotion.service

import com.firstpenguin.app.domain.emotion.dto.TagDto
import com.firstpenguin.app.domain.emotion.model.EmotionRange
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
    fun getEmotionTags(value: Int): List<Tag> {
        val emotionRange = getEmotionRange(value)

        return tagRepository.getEmotionTagsByEmotionRangeId(emotionRange.id)
    }

    fun getEmotionRange(value: Int): EmotionRange =
        emotionRangeRepository.getEmotionRange(value)
            ?: throw CustomException(ErrorCode.EMOTION_RANGE_NOT_FOUND)

    fun getToneTags(): List<Tag> = tagRepository.getToneTags()

    fun selectEmotionTags(emotionTagIds: List<Long>): List<TagDto> {
        val emotionTags = tagRepository.getEmotionTagsByTagIdsIn(emotionTagIds)

        validateEmotionTags(emotionTags, emotionTagIds)
        validateSameEmotionRange(emotionTags)

        return emotionTags.map {
            TagDto(
                id = it.id,
                label = it.label,
                type = it.type,
                emotionRangeId = it.emotionRangeId,
            )
        }
    }

    fun selectToneTags(toneTagIds: List<Long>): List<TagDto> {
        val toneTags = tagRepository.getToneTagsByTagIdsIn(toneTagIds)

        validateToneTags(toneTags, toneTagIds)

        return toneTags.map {
            TagDto(
                id = it.id,
                label = it.label,
                type = it.type,
                emotionRangeId = it.emotionRangeId,
            )
        }
    }

    fun validateEmotionTagsInRange(
        tagIds: List<Long>,
        emotionRangeId: Long,
    ) {
        val emotionTags = tagRepository.getEmotionTagsByTagIdsIn(tagIds)

        validateEmotionTags(emotionTags, tagIds)
        validateSameEmotionRange(emotionTags)

        if (emotionTags.any { tag -> tag.emotionRangeId != emotionRangeId }) {
            throw CustomException(ErrorCode.INVALID_EMOTION_TAG_RANGE)
        }
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
