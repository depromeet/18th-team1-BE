package com.firstpenguin.app.domain.diary.usecase

import com.firstpenguin.app.domain.diary.dto.DiaryPeriodResponse
import com.firstpenguin.app.domain.diary.service.DiaryService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class DiaryUseCase(
    private val diaryService: DiaryService,
) {
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
}
