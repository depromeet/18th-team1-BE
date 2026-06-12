package com.firstpenguin.app.domain.emotion.service

import com.firstpenguin.app.domain.emotion.model.EmotionRange
import com.firstpenguin.app.domain.emotion.model.Tag
import com.firstpenguin.app.domain.emotion.repository.EmotionRangeRepository
import com.firstpenguin.app.domain.emotion.repository.TagRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service

@Service
class EmotionService(
    private val emotionRangeRepository: EmotionRangeRepository,
    private val tagRepository: TagRepository,
) {
    fun getEmotionTags(value: Int): List<Tag> {
        val emotionRange = getEmotionRange(value)

        return tagRepository.getEmotionTagsByEmotionRangeId(emotionRange.id)
    }

    fun getNeedTags(): List<Tag> = tagRepository.getNeedTags()

    fun getEmotionTagsAndNeedTagByIds(tagIds: List<Long>): Pair<List<Tag>, Tag?> =
        tagRepository.getEmotionTagsByTagIdsIn(tagIds) to tagRepository.getNeedTagByTagIdsIn(tagIds)

    fun validateTags(
        emotionRangeId: Long,
        emotionTagIds: List<Long>,
        needTagId: Long?,
    ) {
        validateEmotionTags(emotionTagIds, emotionRangeId)
        validateNeedTag(needTagId)
    }

    private fun validateEmotionTags(
        emotionTagIds: List<Long>,
        emotionRangeId: Long,
    ) {
        val emotionTags = tagRepository.getEmotionTagsByTagIdsIn(emotionTagIds)

        validateEmotionTagIds(emotionTags, emotionTagIds)
        validateSameEmotionRange(emotionTags, emotionRangeId)
    }

    private fun validateNeedTag(needTagId: Long?) {
        if (needTagId == null) return

        tagRepository.getNeedTagByTagId(needTagId)
            ?: throw CustomException(ErrorCode.INVALID_NEED_TAG)
    }

    private fun getEmotionRange(value: Int): EmotionRange =
        emotionRangeRepository.getEmotionRange(value)
            ?: throw CustomException(ErrorCode.EMOTION_RANGE_NOT_FOUND)

    private fun validateEmotionTagIds(
        emotionTags: List<Tag>,
        emotionTagIds: List<Long>,
    ) {
        if (emotionTags.size != emotionTagIds.distinct().size) {
            throw CustomException(ErrorCode.INVALID_EMOTION_TAG)
        }
    }

    private fun validateSameEmotionRange(
        emotionTags: List<Tag>,
        emotionRangeId: Long,
    ) {
        val rangeIds = emotionTags.mapNotNull { tag -> tag.emotionRangeId }.toSet()

        if (rangeIds.size != 1 || rangeIds.first() != emotionRangeId) {
            throw CustomException(ErrorCode.INVALID_EMOTION_TAG_RANGE)
        }
    }
}
