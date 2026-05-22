package com.firstpenguin.app.domain.diary.usecase

import com.firstpenguin.app.domain.diary.dto.CreateDiaryRequest
import com.firstpenguin.app.domain.diary.dto.CreateDiaryResponse
import com.firstpenguin.app.domain.diary.dto.DiaryDetailResponse
import com.firstpenguin.app.domain.diary.dto.DiaryPeriodResponse
import com.firstpenguin.app.domain.diary.dto.UpdateDiaryContentRequest
import com.firstpenguin.app.domain.diary.service.DiaryService
import com.firstpenguin.app.domain.diary.service.DiaryShareImageService
import com.firstpenguin.app.domain.diary.service.DiaryTagService
import com.firstpenguin.app.domain.emotion.service.EmotionService
import com.firstpenguin.app.domain.image.service.ImageService
import com.firstpenguin.app.domain.recommendation.service.RecommendationService
import com.firstpenguin.app.global.enums.ImageOwner
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class DiaryUseCase(
    private val diaryService: DiaryService,
    private val imageService: ImageService,
    private val recommendationService: RecommendationService,
    private val emotionService: EmotionService,
    private val diaryShareImageService: DiaryShareImageService,
    private val diaryTagService: DiaryTagService,
) {
    @Transactional
    fun createDiary(
        userId: Long,
        request: CreateDiaryRequest,
    ): CreateDiaryResponse {
        diaryService.validateCanCreateTodayDiary(userId)
        val dailyRecommendation =
            recommendationService.validateDailyRecommendationQuote(
                userId = userId,
                dailyRecommendationId = request.dailyRecommendationId,
                quoteId = request.quoteId,
            )
        val emotionRange = emotionService.getEmotionRange(request.emotionValue)

        recommendationService.validateSelectedEmotionRange(
            dailyRecommendation = dailyRecommendation,
            emotionRangeId = emotionRange.id,
        )

        val createdDiary =
            diaryService.createDiary(
                userId = userId,
                emotionValue = request.emotionValue,
                quoteId = request.quoteId,
                content = request.content?.takeIf { it.isNotBlank() },
            )

        val recommendationTagIds =
            recommendationService
                .getRecommendationTags(request.dailyRecommendationId)
                .map { recommendationTag -> recommendationTag.tagId }

        diaryTagService.createDiaryTags(createdDiary.diaryId, recommendationTagIds)
        imageService.saveImages(request.imageIds, ImageOwner.DIARY, createdDiary.diaryId)

        return CreateDiaryResponse(createdDiary.diaryId, createdDiary.createdAt)
    }

    @Transactional
    fun deleteDiary(
        userId: Long,
        diaryId: Long,
    ) {
        val diary = diaryService.getById(diaryId)
        diaryService.validateDiaryOwner(ownerId = diary.userId, userId = userId)
        val today = LocalDate.now()
        diaryService.validateTodayDiary(
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
        diaryService.validateDiaryOwner(ownerId = diary.userId, userId = userId)
        val today = LocalDate.now()
        diaryService.validateTodayDiary(
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
        return getDiary(diaryId)
    }

    @Transactional(readOnly = true)
    fun generateShareImage(
        userId: Long,
        diaryId: Long,
    ): ByteArray {
        val diary = diaryService.getById(diaryId)
        diaryService.validateDiaryOwner(ownerId = diary.userId, userId = userId)

        return diaryShareImageService.generate(diary)
    }

    @Transactional(readOnly = true)
    fun getDiary(diaryId: Long): DiaryDetailResponse {
        val diary = diaryService.getById(diaryId)
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
}
