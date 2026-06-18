package com.firstpenguin.app.domain.monthlysettlement.usecase

import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlement
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementBook
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementEmotionTag
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementSelectedBook
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementSnapshot
import com.firstpenguin.app.domain.monthlysettlement.service.MonthlySettlementService
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MonthlySettlementUseCaseTest {
    private lateinit var monthlySettlementService: MonthlySettlementService
    private lateinit var monthlySettlementUseCase: MonthlySettlementUseCase

    @BeforeEach
    fun setUp() {
        monthlySettlementService = Mockito.mock(MonthlySettlementService::class.java)
        monthlySettlementUseCase = MonthlySettlementUseCase(monthlySettlementService)
    }

    @Test
    fun `월말 결산은 저장본 조회 없이 조합 결과로 응답한다`() {
        val targetMonth = YearMonth.now(SEOUL_ZONE).minusMonths(1)
        val snapshot = snapshot(targetMonth)
        Mockito
            .`when`(monthlySettlementService.createSnapshot(USER_ID, targetMonth))
            .thenReturn(snapshot)

        val response =
            monthlySettlementUseCase.getMonthlySettlement(
                userId = USER_ID,
                year = targetMonth.year,
                month = targetMonth.monthValue,
            )

        assertEquals(SHARED_QUOTE_COUNT, response.sharedQuoteCount)
        assertEquals(GENRE, response.mostFrequentGenre)
        assertEquals(listOf(TAG_LABEL), response.emotionTags.map { emotionTag -> emotionTag.label })
        assertEquals(RECOMMENDATION_MESSAGE, response.recommendationMessage)
        assertEquals(SELECTED_QUOTE_ID, response.monthlyBook?.quoteId)
        assertEquals(BOOK_PURCHASE_LINK, response.monthlyBook?.bookPurchaseLink)
        Mockito.verify(monthlySettlementService).createSnapshot(USER_ID, targetMonth)
        Mockito.verify(monthlySettlementService, Mockito.never()).findSnapshot(USER_ID, targetMonth)
    }

    @Test
    fun `조합 가능한 결산이 없으면 빈 응답을 반환한다`() {
        val targetMonth = YearMonth.now(SEOUL_ZONE).minusMonths(1)
        Mockito
            .`when`(monthlySettlementService.createSnapshot(USER_ID, targetMonth))
            .thenReturn(null)

        val response =
            monthlySettlementUseCase.getMonthlySettlement(
                userId = USER_ID,
                year = targetMonth.year,
                month = targetMonth.monthValue,
            )

        assertEquals(0, response.sharedQuoteCount)
        assertEquals(emptyList(), response.monthlyBooks)
        assertEquals(emptyList(), response.emotionTags)
        assertNull(response.recommendationMessage)
        assertNull(response.monthlyBook)
    }

    @Test
    fun `현재 월도 월말 결산 조회를 허용한다`() {
        val currentMonth = YearMonth.now(SEOUL_ZONE)
        val snapshot = snapshot(currentMonth)
        Mockito
            .`when`(monthlySettlementService.createSnapshot(USER_ID, currentMonth))
            .thenReturn(snapshot)

        val response =
            monthlySettlementUseCase.getMonthlySettlement(
                userId = USER_ID,
                year = currentMonth.year,
                month = currentMonth.monthValue,
            )

        assertEquals(SHARED_QUOTE_COUNT, response.sharedQuoteCount)
        Mockito.verify(monthlySettlementService).createSnapshot(USER_ID, currentMonth)
    }

    @Test
    fun `미래 월은 월말 결산 조회를 차단한다`() {
        val futureMonth = YearMonth.now(SEOUL_ZONE).plusMonths(1)

        val exception =
            assertFailsWith<CustomException> {
                monthlySettlementUseCase.getMonthlySettlement(
                    userId = USER_ID,
                    year = futureMonth.year,
                    month = futureMonth.monthValue,
                )
            }

        assertEquals(ErrorCode.MONTHLY_SETTLEMENT_NOT_AVAILABLE, exception.errorCode)
        Mockito.verifyNoInteractions(monthlySettlementService)
    }

    @Test
    fun `잘못된 월은 입력 오류로 처리한다`() {
        val exception =
            assertFailsWith<CustomException> {
                monthlySettlementUseCase.getMonthlySettlement(
                    userId = USER_ID,
                    year = 2026,
                    month = 13,
                )
            }

        assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
        Mockito.verifyNoInteractions(monthlySettlementService)
    }

    private fun snapshot(yearMonth: YearMonth): MonthlySettlementSnapshot =
        MonthlySettlementSnapshot(
            settlement =
                MonthlySettlement(
                    id = SETTLEMENT_ID,
                    userId = USER_ID,
                    year = yearMonth.year,
                    month = yearMonth.monthValue,
                    sharedQuoteCount = SHARED_QUOTE_COUNT,
                    mostFrequentGenre = GENRE,
                    topEmotionTagId = TAG_ID,
                    topEmotionTagLabel = TAG_LABEL,
                    recommendationMessage = RECOMMENDATION_MESSAGE,
                    monthlyBook =
                        MonthlySettlementSelectedBook(
                            quoteId = SELECTED_QUOTE_ID,
                            bookId = BOOK_ID,
                            quoteContent = QUOTE_CONTENT,
                            title = TITLE,
                            author = AUTHOR,
                            bookCoverImageUrl = BOOK_COVER_IMAGE_URL,
                            genre = GENRE,
                            bookPurchaseLink = BOOK_PURCHASE_LINK,
                        ),
                    createdAt = CREATED_AT,
                ),
            monthlyBooks =
                listOf(
                    MonthlySettlementBook(
                        bookId = BOOK_ID,
                        title = TITLE,
                        author = AUTHOR,
                        bookCoverImageUrl = BOOK_COVER_IMAGE_URL,
                        genre = GENRE,
                        sortOrder = 1,
                    ),
                ),
            emotionTags =
                listOf(
                    MonthlySettlementEmotionTag(
                        tagId = TAG_ID,
                        label = TAG_LABEL,
                        count = TAG_COUNT,
                        sortOrder = 1,
                    ),
                ),
        )

    private companion object {
        val SEOUL_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        const val SETTLEMENT_ID = 1L
        const val USER_ID = 10L
        const val BOOK_ID = 20L
        const val SELECTED_QUOTE_ID = 30L
        const val TAG_ID = 40L
        const val SHARED_QUOTE_COUNT = 27
        const val TAG_COUNT = 5
        const val TAG_LABEL = "무기력한"
        const val GENRE = "추리/미스터리 소설"
        const val TITLE = "홍학의 자리"
        const val AUTHOR = "정해연"
        const val BOOK_COVER_IMAGE_URL = "https://cdn.example.com/book-cover-placeholder.png"
        const val BOOK_PURCHASE_LINK = "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=1"
        const val QUOTE_CONTENT = "어떤 기억은 아물지 않습니다."
        const val RECOMMENDATION_MESSAGE = "무기력한 3월을 보내셨군요. 이 감정과 유사한 문장이 담긴 책을 추천해요."
        val CREATED_AT: LocalDateTime = LocalDateTime.of(2026, 4, 1, 0, 0)
    }
}
