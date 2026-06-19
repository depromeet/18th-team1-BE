package com.firstpenguin.app.domain.monthlysettlement.service

import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlement
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementBook
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementCreateCommand
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementEmotionTag
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementSelectedBook
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementSnapshot
import com.firstpenguin.app.domain.monthlysettlement.repository.MonthlySettlementEmotionAggregationRepository
import com.firstpenguin.app.domain.monthlysettlement.repository.MonthlySettlementQuoteAggregationRepository
import com.firstpenguin.app.domain.monthlysettlement.repository.MonthlySettlementRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

private const val MONTHLY_BOOK_COUNT = 3
private const val EMOTION_TAG_COUNT = 10

@Service
class MonthlySettlementService(
    private val monthlySettlementRepository: MonthlySettlementRepository,
    private val monthlySettlementQuoteAggregationRepository: MonthlySettlementQuoteAggregationRepository,
    private val monthlySettlementEmotionAggregationRepository: MonthlySettlementEmotionAggregationRepository,
) {
    fun findSnapshot(
        userId: Long,
        yearMonth: YearMonth,
    ): MonthlySettlementSnapshot? {
        val settlement =
            monthlySettlementRepository.findByUserIdAndYearMonth(
                userId = userId,
                year = yearMonth.year,
                month = yearMonth.monthValue,
            ) ?: return null

        return MonthlySettlementSnapshot(
            settlement = settlement,
            monthlyBooks = monthlySettlementRepository.findBooks(settlement.id),
            emotionTags = monthlySettlementRepository.findEmotionTags(settlement.id),
        )
    }

    fun createSnapshot(
        userId: Long,
        yearMonth: YearMonth,
    ): MonthlySettlementSnapshot? = createCommand(userId, yearMonth)?.toSnapshot()

    private fun createCommand(
        userId: Long,
        yearMonth: YearMonth,
    ): MonthlySettlementCreateCommand? {
        val start = yearMonth.atDay(1)
        val endExclusive = yearMonth.plusMonths(1).atDay(1)
        val sharedQuoteCount =
            monthlySettlementQuoteAggregationRepository.countSelectedQuotes(userId, start, endExclusive)

        if (sharedQuoteCount == 0) return null

        val emotionTags =
            monthlySettlementEmotionAggregationRepository.findEmotionTagCounts(
                userId,
                start,
                endExclusive,
                EMOTION_TAG_COUNT,
            )
        val topEmotionTag =
            emotionTags.firstOrNull()
                ?: throw CustomException(ErrorCode.MONTHLY_SETTLEMENT_CREATE_FAILED)
        val mostFrequentGenre =
            monthlySettlementQuoteAggregationRepository.findMostFrequentGenre(userId, start, endExclusive)
        val monthlyBooks = findMonthlyBooks(userId, yearMonth, mostFrequentGenre)
        val monthlyBook = findMonthlyBook(userId, start, endExclusive, topEmotionTag)

        return MonthlySettlementCreateCommand(
            userId = userId,
            year = yearMonth.year,
            month = yearMonth.monthValue,
            sharedQuoteCount = sharedQuoteCount,
            mostFrequentGenre = mostFrequentGenre,
            topEmotionTag = topEmotionTag,
            recommendationMessage = recommendationMessage(topEmotionTag.label, yearMonth),
            monthlyBook = monthlyBook,
            monthlyBooks = monthlyBooks,
            emotionTags = emotionTags,
        )
    }

    private fun findMonthlyBook(
        userId: Long,
        start: LocalDate,
        endExclusive: LocalDate,
        topEmotionTag: MonthlySettlementEmotionTag,
    ): MonthlySettlementSelectedBook? =
        monthlySettlementEmotionAggregationRepository
            .findMonthlyBookCandidateByEmotionTagId(
                userId,
                start,
                endExclusive,
                topEmotionTag.tagId,
            ) ?: findMonthlyBookByEmotionRange(userId, start, endExclusive, topEmotionTag.emotionRangeId)

    private fun findMonthlyBookByEmotionRange(
        userId: Long,
        start: LocalDate,
        endExclusive: LocalDate,
        emotionRangeId: Long?,
    ): MonthlySettlementSelectedBook? =
        emotionRangeId?.let {
            monthlySettlementEmotionAggregationRepository.findMonthlyBookCandidateByEmotionRangeId(
                userId,
                start,
                endExclusive,
                it,
            )
        }

    private fun findMonthlyBooks(
        userId: Long,
        yearMonth: YearMonth,
        genre: String?,
    ): List<MonthlySettlementBook> {
        if (genre == null) return emptyList()

        val start = yearMonth.atDay(1)
        val endExclusive = yearMonth.plusMonths(1).atDay(1)
        val selectedBooks =
            monthlySettlementQuoteAggregationRepository.findSelectedBooksByGenre(
                userId = userId,
                start = start,
                endExclusive = endExclusive,
                genre = genre,
                limit = MONTHLY_BOOK_COUNT,
            )
        val fallbackBooks = findFallbackBooks(genre, selectedBooks)

        return (selectedBooks + fallbackBooks)
            .take(MONTHLY_BOOK_COUNT)
            .mapIndexed { index, book -> book.copy(sortOrder = index + 1) }
    }

    private fun findFallbackBooks(
        genre: String,
        selectedBooks: List<MonthlySettlementBook>,
    ): List<MonthlySettlementBook> {
        val shortage = MONTHLY_BOOK_COUNT - selectedBooks.size
        if (shortage <= 0) return emptyList()

        return monthlySettlementQuoteAggregationRepository.findFallbackBooksByGenre(
            genre = genre,
            excludedBookIds = selectedBooks.map { book -> book.bookId },
            limit = shortage,
        )
    }

    private fun recommendationMessage(
        topEmotionTagLabel: String,
        yearMonth: YearMonth,
    ): String = "$topEmotionTagLabel ${yearMonth.monthValue}월을 보내셨군요. 이 감정과 유사한 문장이 담긴 책을 추천해요."

    private fun MonthlySettlementCreateCommand.toSnapshot(): MonthlySettlementSnapshot =
        MonthlySettlementSnapshot(
            settlement =
                MonthlySettlement(
                    id = 0L,
                    userId = userId,
                    year = year,
                    month = month,
                    sharedQuoteCount = sharedQuoteCount,
                    mostFrequentGenre = mostFrequentGenre,
                    topEmotionTagId = topEmotionTag.tagId,
                    topEmotionTagLabel = topEmotionTag.label,
                    recommendationMessage = recommendationMessage,
                    monthlyBook = monthlyBook,
                    createdAt = LocalDateTime.now(),
                ),
            monthlyBooks = monthlyBooks,
            emotionTags = emotionTags,
        )
}
