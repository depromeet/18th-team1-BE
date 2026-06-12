package com.firstpenguin.app.domain.diary.service

import com.firstpenguin.app.domain.diary.model.Diary
import com.firstpenguin.app.domain.diary.repository.DiaryRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service

@Service
class DiaryService(
    private val diaryRepository: DiaryRepository,
) {
    fun getById(id: Long): Diary =
        diaryRepository.findById(id)
            ?: throw CustomException(ErrorCode.DIARY_NOT_FOUND)

    fun validateDiaryOwner(
        ownerId: Long,
        userId: Long,
    ) {
        if (ownerId != userId) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }
    }
}
