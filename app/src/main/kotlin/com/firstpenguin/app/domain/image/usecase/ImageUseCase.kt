package com.firstpenguin.app.domain.image.usecase

import com.firstpenguin.app.domain.image.dto.PresignedUrlRequest
import com.firstpenguin.app.domain.image.dto.PresignedUrlResponse
import com.firstpenguin.app.domain.image.service.ImageService
import com.firstpenguin.app.domain.quote.service.QuoteService
import com.firstpenguin.app.domain.recommendation.model.Recommendation
import com.firstpenguin.app.domain.recommendation.service.RecommendationService
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ImageUseCase(
    private val imageService: ImageService,
    private val quoteService: QuoteService,
    private val recommendationService: RecommendationService,
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
        val recommendations =
            recommendationService
                .findCompletedByUserIdAndRecommendationDateBetween(
                    userId = userId,
                    start = firstOfMonth,
                    end = lastOfMonth,
                )
        val coverImageUrlsByQuoteId =
            quoteService.findBookCoverImageUrlsByQuoteIds(
                recommendations
                    .mapNotNull(Recommendation::quoteId)
                    .distinct(),
            )
        val books = recommendations.toCalendarBooks(coverImageUrlsByQuoteId)

        return when (type) {
            SHARE_VIEW_4_TYPE -> imageService.generateShareView4(firstOfMonth, books)
            SHARE_VIEW_5_TYPE -> imageService.generateShareView5(firstOfMonth, books)
            else -> throw CustomException(ErrorCode.INVALID_INPUT)
        }
    }

    private fun List<Recommendation>.toCalendarBooks(coverImageUrlsByQuoteId: Map<Long, String>): Map<Int, List<String>> =
        groupBy { recommendation -> recommendation.recommendationDate.dayOfMonth }
            .mapValues { (_, recommendations) ->
                recommendations.mapNotNull { recommendation ->
                    recommendation.quoteId?.let(coverImageUrlsByQuoteId::get)
                }
            }

    private companion object {
        const val SHARE_VIEW_4_TYPE = 4
        const val SHARE_VIEW_5_TYPE = 5
    }
}
