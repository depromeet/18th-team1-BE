package com.firstpenguin.app.domain.diary.usecase

import com.firstpenguin.app.domain.diary.dto.CreateDiaryRequest
import com.firstpenguin.app.domain.diary.dto.CreateDiaryResponse
import com.firstpenguin.app.domain.diary.dto.DiaryDetailResponse
import com.firstpenguin.app.domain.diary.dto.DiaryExistsResponse
import com.firstpenguin.app.domain.diary.dto.DiaryPeriodResponse
import com.firstpenguin.app.domain.diary.dto.UpdateDiaryContentRequest
import com.firstpenguin.app.domain.diary.model.CreatedDiary
import com.firstpenguin.app.domain.diary.service.DiaryService
import com.firstpenguin.app.domain.diary.service.DiaryShareImageService
import com.firstpenguin.app.domain.emotion.service.EmotionService
import com.firstpenguin.app.domain.image.service.ImageService
import com.firstpenguin.app.domain.recommendation.model.DailyRecommendation
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
    ) {
    @Transactional
    fun createDiary(
        userId: Long,
        request: CreateDiaryRequest,
    ): CreateDiaryResponse {
        diaryService.validateCanCreateTodayDiary(userId)
        val dailyRecommendation = validateRecommendation(userId, request)
        validateEmotion(dailyRecommendation, request)
        val createdDiary = persistDiary(userId, request)
        persistAssociations(createdDiary.diaryId, request)
        return CreateDiaryResponse(createdDiary.diaryId, createdDiary.createdAt)
    }

    private fun validateRecommendation(
        userId: Long,
        request: CreateDiaryRequest,
    ): DailyRecommendation {
        val dailyRecommendation =
            recommendationService.getDailyRecommendation(request.dailyRecommendationId)

        recommendationService.validateOwner(userId, dailyRecommendation)
        recommendationService.validateTodayRecommendation(dailyRecommendation.recommendationDate)
        recommendationService.validateRecommendedQuote(
            dailyRecommendationId = dailyRecommendation.id,
            quoteId = request.quoteId,
        )

        return dailyRecommendation
    }

    private fun validateEmotion(
        dailyRecommendation: DailyRecommendation,
        request: CreateDiaryRequest,
    ) {
        val emotionRange = emotionService.getEmotionRange(request.emotionIntensity)
        recommendationService.validateSelectedEmotionRange(
            dailyRecommendation = dailyRecommendation,
            emotionRangeId = emotionRange.id,
        )
        emotionService.validateEmotionTagsInRange(
            tagIds = request.tagIds,
            emotionRangeId = emotionRange.id,
        )
    }

    private fun persistDiary(
        userId: Long,
        request: CreateDiaryRequest,
    ): CreatedDiary {
        val content = request.content?.takeIf { it.isNotBlank() }

        return diaryService.createDiary(
            userId = userId,
            emotionIntensity = request.emotionIntensity,
            quoteId = request.quoteId,
            content = content,
        )
    }

    private fun persistAssociations(
        diaryId: Long,
        request: CreateDiaryRequest,
    ) {
        diaryService.createDiaryTags(diaryId, request.tagIds)
        imageService.saveImages(request.imageIds, ImageOwner.DIARY, diaryId)
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
        return getDiary(userId = userId, diaryId = diaryId)
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
    fun hasTodayDiary(userId: Long): DiaryExistsResponse = DiaryExistsResponse(diaryService.hasTodayDiary(userId))

    @Transactional(readOnly = true)
    fun getDiary(
        userId: Long,
        diaryId: Long,
    ): DiaryDetailResponse {
        val diary = diaryService.getById(diaryId)
        diaryService.validateDiaryOwner(ownerId = diary.userId, userId = userId)
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
