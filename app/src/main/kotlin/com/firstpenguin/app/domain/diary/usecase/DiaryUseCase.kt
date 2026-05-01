package com.firstpenguin.app.domain.diary.usecase

import com.firstpenguin.app.domain.diary.dto.DiaryDetailResponse
import com.firstpenguin.app.domain.diary.dto.DiaryPeriodResponse
import com.firstpenguin.app.domain.diary.service.DiaryService
import com.firstpenguin.app.domain.image.service.ImageService
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class DiaryUseCase(
    private val diaryService: DiaryService,
    private val imageService: ImageService,
) {
    @Transactional(readOnly = true)
    fun getDiary(
        userId: Long,
        diaryId: Long,
    ): DiaryDetailResponse {
        val diary = diaryService.getById(diaryId)
        validateDiaryOwner(ownerId = diary.userId, userId = userId)
        val diaryImageUrl = diary.diaryImageId?.let(imageService::findUrlById)

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
}
