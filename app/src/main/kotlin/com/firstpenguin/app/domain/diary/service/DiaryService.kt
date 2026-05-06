package com.firstpenguin.app.domain.diary.service

import com.firstpenguin.app.domain.diary.model.CreatedDiary
import com.firstpenguin.app.domain.diary.model.Diary
import com.firstpenguin.app.domain.diary.repository.DiaryRepository
import com.firstpenguin.app.domain.diary.repository.DiaryTagRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class DiaryService(
    private val diaryRepository: DiaryRepository,
    private val diaryTagRepository: DiaryTagRepository,
) {
    fun createDiary(
        userId: Long,
        emotionIntensity: Int,
        quoteId: Long,
        content: String?,
    ): CreatedDiary = diaryRepository.create(userId, emotionIntensity, quoteId, content)

    fun getById(id: Long): Diary =
        diaryRepository.findById(id)
            ?: throw CustomException(ErrorCode.DIARY_NOT_FOUND)

    fun updateContent(
        id: Long,
        userId: Long,
        content: String?,
        start: LocalDateTime,
        end: LocalDateTime,
    ) {
        val updatedCount =
            diaryRepository.updateContent(
                id = id,
                userId = userId,
                content = content,
                start = start,
                end = end,
            )

        if (updatedCount == 0) {
            throw CustomException(ErrorCode.DIARY_UPDATE_NOT_ALLOWED)
        }
    }

    fun delete(
        id: Long,
        userId: Long,
        start: LocalDateTime,
        end: LocalDateTime,
    ) {
        val deletedCount =
            diaryRepository.delete(
                id = id,
                userId = userId,
                start = start,
                end = end,
            )

        if (deletedCount == 0) {
            throw CustomException(ErrorCode.DIARY_DELETE_NOT_ALLOWED)
        }
    }

    fun findByPeriod(
        userId: Long,
        start: LocalDate,
        end: LocalDate,
    ): List<Diary> {
        if (start.isAfter(end)) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }

        return diaryRepository.findAllByUserIdAndCreatedAtBetween(
            userId = userId,
            start = start.atStartOfDay(),
            end = end.plusDays(1).atStartOfDay(),
        )
    }

    fun createDiaryTags(
        diaryId: Long,
        tagIds: List<Long>,
    ) {
        diaryTagRepository.createAll(diaryId, tagIds)
    }

    fun validateDiaryOwner(
        ownerId: Long,
        userId: Long,
    ) {
        if (ownerId != userId) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }
    }

    fun validateTodayDiary(
        createdAt: LocalDate,
        today: LocalDate,
        errorCode: ErrorCode,
    ) {
        if (createdAt != today) {
            throw CustomException(errorCode)
        }
    }

    fun validateCanCreateTodayDiary(userId: Long) {
        val today = LocalDate.now()
        val hasTodayDiary =
            diaryRepository.existsByUserIdAndCreatedAtBetween(
                userId = userId,
                start = today.atStartOfDay(),
                end = today.plusDays(1).atStartOfDay(),
            )

        if (hasTodayDiary) {
            throw CustomException(ErrorCode.DIARY_ALREADY_EXISTS)
        }
    }
}
