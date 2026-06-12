package com.firstpenguin.app.domain.diary.usecase

import com.firstpenguin.app.domain.diary.service.DiaryService
import com.firstpenguin.app.domain.diary.service.DiaryShareImageService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DiaryUseCase(
    private val diaryService: DiaryService,
    private val diaryShareImageService: DiaryShareImageService,
) {
    @Transactional(readOnly = true)
    fun generateShareImage(
        userId: Long,
        diaryId: Long,
    ): ByteArray {
        val diary = diaryService.getById(diaryId)
        diaryService.validateDiaryOwner(ownerId = diary.userId, userId = userId)

        return diaryShareImageService.generate(diary)
    }
}
