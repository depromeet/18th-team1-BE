package com.firstpenguin.app.domain.home.useCase

import com.firstpenguin.app.domain.book.service.BookService
import com.firstpenguin.app.domain.diary.service.DiaryService
import com.firstpenguin.app.domain.home.dto.HomeSummaryResponse
import com.firstpenguin.app.domain.home.dto.MonthlyDiaryResponse
import com.firstpenguin.app.domain.image.service.ImageService
import com.firstpenguin.app.domain.quote.dto.QuoteResponse
import com.firstpenguin.app.domain.quote.service.QuoteService
import com.firstpenguin.app.global.enums.ImageOwner
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class HomeUseCase(
    private val quoteService: QuoteService,
    private val bookService: BookService,
    private val imageService: ImageService,
    private val diaryService: DiaryService,
) {
    @Transactional(readOnly = true)
    fun getSummary(userId: Long): HomeSummaryResponse {
        val today = LocalDate.now()
        val monthStart = today.withDayOfMonth(1)
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth())

        val monthlyDiaries = diaryService.findByPeriod(userId = userId, start = monthStart, end = monthEnd)
        val responses = monthlyDiaries.map(MonthlyDiaryResponse::from)
        val todayDiary = responses.firstOrNull { it.createdAt == today }
        val totalDiaryCount = diaryService.countByUserId(userId)

        return HomeSummaryResponse(
            todayDiary = todayDiary,
            monthlyDiaries = responses,
            totalDiaryCount = totalDiaryCount,
        )
    }

    @Transactional(readOnly = true)
    fun getRandomQuote(): QuoteResponse {
        val randomQuote = quoteService.getRandomQuote()

        val book = bookService.findBookById(randomQuote.bookId)
        val bookCoverImage =
            imageService
                .findUrlsByOwnerIdAndOwnerType(ImageOwner.BOOK, randomQuote.bookId)
                .firstOrNull()
                ?: throw CustomException(ErrorCode.IMAGE_NOT_FOUND)

        return QuoteResponse(
            quoteId = randomQuote.id,
            bookId = book.id,
            content = randomQuote.content,
            title = book.title,
            author = book.author,
            image = bookCoverImage,
        )
    }
}
