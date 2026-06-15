package com.firstpenguin.app.domain.monthlysettlement.service

import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementBook
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementCreateCommand
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementEmotionTag
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementSnapshot
import com.firstpenguin.app.domain.monthlysettlement.repository.MonthlySettlementCommandRepository
import com.firstpenguin.app.domain.monthlysettlement.repository.MonthlySettlementEmotionAggregationRepository
import com.firstpenguin.app.domain.monthlysettlement.repository.MonthlySettlementQuoteAggregationRepository
import com.firstpenguin.app.domain.monthlysettlement.repository.MonthlySettlementRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service
import java.time.YearMonth

private const val MONTHLY_BOOK_COUNT = 3
private const val EMOTION_TAG_COUNT = 10

@Service
class MonthlySettlementService(
    private val monthlySettlementRepository: MonthlySettlementRepository,
    private val monthlySettlementCommandRepository: MonthlySettlementCommandRepository,
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
    ): MonthlySettlementSnapshot? {
        val draft = createCommand(userId, yearMonth) ?: return null
        val monthlySettlementId = monthlySettlementCommandRepository.insertMonthlySettlement(draft)

        return findCreatedSnapshot(
            userId = userId,
            yearMonth = yearMonth,
            monthlySettlementId = monthlySettlementId,
        )
    }

    private fun findCreatedSnapshot(
        userId: Long,
        yearMonth: YearMonth,
        monthlySettlementId: Long?,
    ): MonthlySettlementSnapshot? {
        if (monthlySettlementId == null) return findSnapshot(userId, yearMonth)

        return checkNotNull(findSnapshot(userId, yearMonth))
    }

    private fun createCommand(
        userId: Long,
        yearMonth: YearMonth,
    ): MonthlySettlementCreateCommand? {
        val start = yearMonth.atDay(1)
        val endExclusive = yearMonth.plusMonths(1).atDay(1)
        val sharedQuoteCount =
            monthlySettlementQuoteAggregationRepository.countRecommendedQuotes(userId, start, endExclusive)

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
        val monthlyBook =
            monthlySettlementEmotionAggregationRepository.findMonthlyBookCandidateByEmotionTagId(
                userId = userId,
                start = start,
                endExclusive = endExclusive,
                tagId = topEmotionTag.tagId,
            )

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

    private fun findMonthlyBooks(
        userId: Long,
        yearMonth: YearMonth,
        genre: String?,
    ): List<MonthlySettlementBook> {
        if (genre == null) return emptyList()

        val start = yearMonth.atDay(1)
        val endExclusive = yearMonth.plusMonths(1).atDay(1)
        val recommendedBooks =
            monthlySettlementQuoteAggregationRepository.findRecommendedBooksByGenre(
                userId = userId,
                start = start,
                endExclusive = endExclusive,
                genre = genre,
                limit = MONTHLY_BOOK_COUNT,
            )
        val fallbackBooks = findFallbackBooks(genre, recommendedBooks)

        return (recommendedBooks + fallbackBooks)
            .take(MONTHLY_BOOK_COUNT)
            .mapIndexed { index, book -> book.copy(sortOrder = index + 1) }
    }

    private fun findFallbackBooks(
        genre: String,
        recommendedBooks: List<MonthlySettlementBook>,
    ): List<MonthlySettlementBook> {
        val shortage = MONTHLY_BOOK_COUNT - recommendedBooks.size
        if (shortage <= 0) return emptyList()

        return monthlySettlementQuoteAggregationRepository.findFallbackBooksByGenre(
            genre = genre,
            excludedBookIds = recommendedBooks.map { book -> book.bookId },
            limit = shortage,
        )
    }

    private fun recommendationMessage(
        topEmotionTagLabel: String,
        yearMonth: YearMonth,
    ): String = "$topEmotionTagLabel ${yearMonth.monthValue}월을 보내셨군요. 이 감정과 유사한 문장이 담긴 책을 추천해요."
}
