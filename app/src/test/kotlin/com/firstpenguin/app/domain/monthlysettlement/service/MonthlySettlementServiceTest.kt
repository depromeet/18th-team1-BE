package com.firstpenguin.app.domain.monthlysettlement.service

import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementEmotionTag
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementSelectedBook
import com.firstpenguin.app.domain.monthlysettlement.repository.MonthlySettlementEmotionAggregationRepository
import com.firstpenguin.app.domain.monthlysettlement.repository.MonthlySettlementQuoteAggregationRepository
import com.firstpenguin.app.domain.monthlysettlement.repository.MonthlySettlementRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertEquals

class MonthlySettlementServiceTest {
    private lateinit var monthlySettlementRepository: MonthlySettlementRepository
    private lateinit var monthlySettlementQuoteAggregationRepository:
        MonthlySettlementQuoteAggregationRepository
    private lateinit var monthlySettlementEmotionAggregationRepository:
        MonthlySettlementEmotionAggregationRepository
    private lateinit var monthlySettlementService: MonthlySettlementService

    @BeforeEach
    fun setUp() {
        monthlySettlementRepository = Mockito.mock(MonthlySettlementRepository::class.java)
        monthlySettlementQuoteAggregationRepository =
            Mockito.mock(MonthlySettlementQuoteAggregationRepository::class.java)
        monthlySettlementEmotionAggregationRepository =
            Mockito.mock(MonthlySettlementEmotionAggregationRepository::class.java)
        monthlySettlementService =
            MonthlySettlementService(
                monthlySettlementRepository,
                monthlySettlementQuoteAggregationRepository,
                monthlySettlementEmotionAggregationRepository,
            )
    }

    @Test
    fun `정확한 1위 감정 태그 후보가 없으면 같은 감정 범위 후보로 월말 책을 선택한다`() {
        stubMonthlySettlementInputs()
        Mockito
            .`when`(
                monthlySettlementEmotionAggregationRepository.findMonthlyBookCandidateByEmotionTagId(
                    USER_ID,
                    START,
                    END_EXCLUSIVE,
                    TAG_ID,
                ),
            ).thenReturn(null)
        Mockito
            .`when`(
                monthlySettlementEmotionAggregationRepository.findMonthlyBookCandidateByEmotionRangeId(
                    USER_ID,
                    START,
                    END_EXCLUSIVE,
                    EMOTION_RANGE_ID,
                ),
            ).thenReturn(selectedBook())

        val snapshot = monthlySettlementService.createSnapshot(USER_ID, YEAR_MONTH)

        assertEquals(selectedBook(), snapshot?.settlement?.monthlyBook)
        assertEquals(listOf(topEmotionTag()), snapshot?.emotionTags)
    }

    @Test
    fun `정확한 1위 감정 태그 후보가 있으면 같은 감정 범위 후보를 조회하지 않는다`() {
        stubMonthlySettlementInputs()
        Mockito
            .`when`(
                monthlySettlementEmotionAggregationRepository.findMonthlyBookCandidateByEmotionTagId(
                    USER_ID,
                    START,
                    END_EXCLUSIVE,
                    TAG_ID,
                ),
            ).thenReturn(selectedBook())

        val snapshot = monthlySettlementService.createSnapshot(USER_ID, YEAR_MONTH)

        assertEquals(selectedBook(), snapshot?.settlement?.monthlyBook)
        Mockito
            .verify(monthlySettlementEmotionAggregationRepository, Mockito.never())
            .findMonthlyBookCandidateByEmotionRangeId(USER_ID, START, END_EXCLUSIVE, EMOTION_RANGE_ID)
    }

    private fun stubMonthlySettlementInputs() {
        Mockito
            .`when`(monthlySettlementQuoteAggregationRepository.countSelectedQuotes(USER_ID, START, END_EXCLUSIVE))
            .thenReturn(SHARED_QUOTE_COUNT)
        Mockito
            .`when`(
                monthlySettlementEmotionAggregationRepository.findEmotionTagCounts(
                    USER_ID,
                    START,
                    END_EXCLUSIVE,
                    EMOTION_TAG_LIMIT,
                ),
            ).thenReturn(listOf(topEmotionTag()))
        Mockito
            .`when`(monthlySettlementQuoteAggregationRepository.findMostFrequentGenre(USER_ID, START, END_EXCLUSIVE))
            .thenReturn(null)
    }

    private fun topEmotionTag(): MonthlySettlementEmotionTag =
        MonthlySettlementEmotionTag(
            tagId = TAG_ID,
            emotionRangeId = EMOTION_RANGE_ID,
            label = TAG_LABEL,
            count = TAG_COUNT,
            sortOrder = 1,
        )

    private fun selectedBook(): MonthlySettlementSelectedBook =
        MonthlySettlementSelectedBook(
            quoteId = SELECTED_QUOTE_ID,
            bookId = BOOK_ID,
            quoteContent = QUOTE_CONTENT,
            title = TITLE,
            author = AUTHOR,
            bookCoverImageUrl = BOOK_COVER_IMAGE_URL,
            genre = GENRE,
            bookPurchaseLink = BOOK_PURCHASE_LINK,
        )

    private companion object {
        const val USER_ID = 1L
        const val YEAR = 2026
        const val MONTH = 3
        const val SHARED_QUOTE_COUNT = 27
        const val TAG_ID = 10L
        const val EMOTION_RANGE_ID = 1L
        const val TAG_LABEL = "무기력한"
        const val TAG_COUNT = 5
        const val EMOTION_TAG_LIMIT = 10
        const val SELECTED_QUOTE_ID = 20L
        const val BOOK_ID = 30L
        const val QUOTE_CONTENT = "어떤 기억은 아물지 않습니다."
        const val TITLE = "홍학의 자리"
        const val AUTHOR = "정해연"
        const val BOOK_COVER_IMAGE_URL = "https://cdn.example.com/book-cover-placeholder.png"
        const val GENRE = "추리/미스터리 소설"
        const val BOOK_PURCHASE_LINK = "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=1"
        val YEAR_MONTH: YearMonth = YearMonth.of(YEAR, MONTH)
        val START: LocalDate = YEAR_MONTH.atDay(1)
        val END_EXCLUSIVE: LocalDate = YEAR_MONTH.plusMonths(1).atDay(1)
    }
}
