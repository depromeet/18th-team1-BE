package com.firstpenguin.app.domain.image.usecase

import com.firstpenguin.app.domain.diary.model.Diary
import com.firstpenguin.app.domain.diary.service.DiaryService
import com.firstpenguin.app.domain.image.dto.PresignedUrlRequest
import com.firstpenguin.app.domain.image.dto.PresignedUrlResponse
import com.firstpenguin.app.domain.image.service.ImageService
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ImageUseCase(
    private val imageService: ImageService,
    private val diaryService: DiaryService,
) {
    fun issuePresignedUrl(request: PresignedUrlRequest): PresignedUrlResponse {
        val (presignedUrl, imageId) = imageService.issue(request.type, request.contentType)
        return PresignedUrlResponse(presignedUrl, imageId)
    }

    fun generateQuoteShareImage(
        type: Int,
        createdAt: LocalDate,
        quote: String,
        title: String,
        author: String,
        coverImageUrl: String?,
    ): ByteArray =
        imageService.generateQuoteShareImage(
            type = type,
            createdAt = createdAt,
            quote = quote,
            title = title,
            author = author,
            coverImageUrl = coverImageUrl,
        )

    fun generateCalendarShareImage(
        userId: Long,
        type: Int,
        year: Int,
        month: Int,
    ): ByteArray {
        val firstOfMonth = LocalDate.of(year, month, 1)
        val lastOfMonth = firstOfMonth.withDayOfMonth(firstOfMonth.lengthOfMonth())
        val books =
            diaryService
                .findByPeriod(userId, firstOfMonth, lastOfMonth)
                .toCalendarBooks()
        return when (type) {
            SHARE_VIEW_4_TYPE -> imageService.generateShareView4(firstOfMonth, books)
            SHARE_VIEW_5_TYPE -> imageService.generateShareView5(firstOfMonth, books)
            else -> throw CustomException(ErrorCode.INVALID_INPUT)
        }
    }

    private fun List<Diary>.toCalendarBooks(): Map<Int, List<String>> =
        groupBy { it.createdAt.dayOfMonth }
            .mapValues { (_, dayDiaries) -> dayDiaries.map { it.coverImageUrl } }

    private companion object {
        const val SHARE_VIEW_4_TYPE = 4
        const val SHARE_VIEW_5_TYPE = 5
    }
}
