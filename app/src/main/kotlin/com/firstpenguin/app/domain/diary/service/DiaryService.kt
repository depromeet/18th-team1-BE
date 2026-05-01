package com.firstpenguin.app.domain.diary.service

import com.firstpenguin.app.domain.diary.model.Diary
import com.firstpenguin.app.domain.diary.repository.DiaryRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DiaryService(
    private val diaryRepository: DiaryRepository,
) {
    fun getById(id: Long): Diary =
        diaryRepository.findById(id)
            ?: throw CustomException(ErrorCode.DIARY_NOT_FOUND)

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
}
