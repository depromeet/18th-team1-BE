package com.firstpenguin.app.domain.diary.service

import com.firstpenguin.app.domain.diary.repository.DiaryTagRepository
import org.springframework.stereotype.Service

@Service
class DiaryTagService(
    private val diaryTagRepository: DiaryTagRepository,
) {
    fun createDiaryTags(
        diaryId: Long,
        tagIds: List<Long>,
    ) {
        diaryTagRepository.createAll(diaryId, tagIds)
    }
}
