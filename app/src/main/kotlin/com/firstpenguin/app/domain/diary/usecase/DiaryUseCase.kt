package com.firstpenguin.app.domain.diary.usecase

import com.firstpenguin.app.domain.diary.dto.DiaryDetailResponse
import com.firstpenguin.app.domain.diary.dto.DiaryPeriodResponse
import com.firstpenguin.app.domain.diary.dto.UpdateDiaryContentRequest
import com.firstpenguin.app.domain.diary.service.DiaryService
import com.firstpenguin.app.domain.diary.service.DiaryShareImageService
import com.firstpenguin.app.domain.image.service.ImageService
import com.firstpenguin.app.global.enums.ImageOwner
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class DiaryUseCase(
    private val diaryService: DiaryService,
    private val imageService: ImageService,
    private val diaryShareImageService: DiaryShareImageService,
) {
    @Transactional
    fun deleteDiary(
        userId: Long,
        diaryId: Long,
    ) {
        val diary = diaryService.getById(diaryId)
        validateDiaryOwner(ownerId = diary.userId, userId = userId)
        val today = LocalDate.now()
        validateTodayDiary(
            createdAt = diary.createdAt.toLocalDate(),
            today = today,
            errorCode = ErrorCode.DIARY_DELETE_NOT_ALLOWED,
        )

        diaryService.delete(
            id = diaryId,
            userId = userId,
            start = today.atStartOfDay(),
            end = today.plusDays(1).atStartOfDay(),
        )
    }

    @Transactional
    fun updateDiaryContent(
        userId: Long,
        diaryId: Long,
        request: UpdateDiaryContentRequest,
    ): DiaryDetailResponse {
        val diary = diaryService.getById(diaryId)
        validateDiaryOwner(ownerId = diary.userId, userId = userId)
        val today = LocalDate.now()
        validateTodayDiary(
            createdAt = diary.createdAt.toLocalDate(),
            today = today,
            errorCode = ErrorCode.DIARY_UPDATE_NOT_ALLOWED,
        )

        diaryService.updateContent(
            id = diaryId,
            userId = userId,
            content = request.content,
            start = today.atStartOfDay(),
            end = today.plusDays(1).atStartOfDay(),
        )
        return getDiary(userId = userId, diaryId = diaryId)
    }

    @Transactional(readOnly = true)
    fun generateShareImage(
        userId: Long,
        diaryId: Long,
    ): ByteArray {
        val diary = diaryService.getById(diaryId)
        validateDiaryOwner(ownerId = diary.userId, userId = userId)

        return diaryShareImageService.generate(diary)
    }

    @Transactional(readOnly = true)
    fun getDiary(
        userId: Long,
        diaryId: Long,
    ): DiaryDetailResponse {
        val diary = diaryService.getById(diaryId)
        validateDiaryOwner(ownerId = diary.userId, userId = userId)
        val diaryImageUrl =
            imageService
                .findUrlsByOwnerIdAndOwnerType(
                    ownerType = ImageOwner.DIARY,
                    ownerId = diary.id,
                ).firstOrNull()

        return DiaryDetailResponse.from(diary, diaryImageUrl)
    }

    @Transactional(readOnly = true)
    fun getDiariesByPeriod(
        userId: Long,
        start: LocalDate,
        end: LocalDate,
    ): DiaryPeriodResponse {
        val diaries =
            diaryService.findByPeriod(
                userId = userId,
                start = start,
                end = end,
            )

        return DiaryPeriodResponse.from(start, end, diaries)
    }

    private fun validateDiaryOwner(
        ownerId: Long,
        userId: Long,
    ) {
        if (ownerId != userId) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }
    }

    private fun validateTodayDiary(
        createdAt: LocalDate,
        today: LocalDate,
        errorCode: ErrorCode,
    ) {
        if (createdAt != today) {
            throw CustomException(errorCode)
        }
    }
}
