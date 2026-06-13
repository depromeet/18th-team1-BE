package com.firstpenguin.app.domain.quote.usecase

import com.firstpenguin.app.domain.quote.model.ScrappedQuote
import com.firstpenguin.app.domain.quote.service.QuoteScrapService
import com.firstpenguin.app.domain.quote.service.QuoteService
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class QuoteScrapUseCaseTest {
    private lateinit var quoteService: QuoteService
    private lateinit var quoteScrapService: QuoteScrapService
    private lateinit var quoteScrapUseCase: QuoteScrapUseCase

    @BeforeEach
    fun setUp() {
        quoteService = Mockito.mock(QuoteService::class.java)
        quoteScrapService = Mockito.mock(QuoteScrapService::class.java)
        quoteScrapUseCase = QuoteScrapUseCase(quoteService, quoteScrapService)
    }

    @Test
    fun `문장이 존재하면 스크랩 상태로 설정한다`() {
        quoteScrapUseCase.setQuoteScrap(USER_ID, QUOTE_ID)

        Mockito.verify(quoteService).findQuoteById(QUOTE_ID)
        Mockito.verify(quoteScrapService).setQuoteScrap(USER_ID, QUOTE_ID)
    }

    @Test
    fun `문장이 존재하면 스크랩을 취소한다`() {
        quoteScrapUseCase.deleteQuoteScrap(USER_ID, QUOTE_ID)

        Mockito.verify(quoteService).findQuoteById(QUOTE_ID)
        Mockito.verify(quoteScrapService).deleteQuoteScrap(USER_ID, QUOTE_ID)
    }

    @Test
    fun `존재하지 않는 문장 스크랩 생성 요청은 실패한다`() {
        Mockito.`when`(quoteService.findQuoteById(QUOTE_ID)).thenThrow(quoteNotFoundException())

        val exception =
            assertFailsWith<CustomException> {
                quoteScrapUseCase.setQuoteScrap(USER_ID, QUOTE_ID)
            }

        assertEquals(ErrorCode.QUOTE_NOT_FOUND, exception.errorCode)
        Mockito.verifyNoInteractions(quoteScrapService)
    }

    @Test
    fun `존재하지 않는 문장 스크랩 취소 요청은 실패한다`() {
        Mockito.`when`(quoteService.findQuoteById(QUOTE_ID)).thenThrow(quoteNotFoundException())

        val exception =
            assertFailsWith<CustomException> {
                quoteScrapUseCase.deleteQuoteScrap(USER_ID, QUOTE_ID)
            }

        assertEquals(ErrorCode.QUOTE_NOT_FOUND, exception.errorCode)
        Mockito.verifyNoInteractions(quoteScrapService)
    }

    @Test
    fun `스크랩 목록과 총 개수를 응답한다`() {
        val quote = scrappedQuote(QUOTE_ID)
        Mockito.`when`(quoteScrapService.getScrappedQuotes(USER_ID, null, FETCH_LIMIT)).thenReturn(listOf(quote))
        Mockito.`when`(quoteScrapService.countQuoteScraps(USER_ID)).thenReturn(TOTAL_COUNT)

        val response = quoteScrapUseCase.getScrappedQuotes(USER_ID, cursor = null, limit = LIMIT)

        assertEquals(TOTAL_COUNT, response.totalCount)
        assertEquals(1, response.quotes.size)
        assertEquals(QUOTE_ID, response.quotes.first().quoteId)
        assertEquals(false, response.hasNext)
        assertNull(response.nextCursor)
    }

    @Test
    fun `조회 결과가 limit보다 많으면 다음 커서를 만든다`() {
        val quotes = listOf(scrappedQuote(10L), scrappedQuote(9L))
        Mockito.`when`(quoteScrapService.getScrappedQuotes(USER_ID, null, 2)).thenReturn(quotes)
        Mockito.`when`(quoteScrapService.countQuoteScraps(USER_ID)).thenReturn(TOTAL_COUNT)

        val response = quoteScrapUseCase.getScrappedQuotes(USER_ID, cursor = null, limit = 1)

        assertEquals(true, response.hasNext)
        assertEquals(EXPECTED_NEXT_CURSOR, response.nextCursor)
        assertEquals(1, response.quotes.size)
    }

    @Test
    fun `limit가 최대값을 초과하면 실패한다`() {
        val exception =
            assertFailsWith<CustomException> {
                quoteScrapUseCase.getScrappedQuotes(USER_ID, cursor = null, limit = 51)
            }

        assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
    }

    @Test
    fun `여러 스크랩을 취소한다`() {
        val quoteIds = listOf(10L, 20L)

        quoteScrapUseCase.deleteQuoteScraps(USER_ID, quoteIds)

        Mockito.verify(quoteScrapService).deleteQuoteScraps(USER_ID, quoteIds)
    }

    @Test
    fun `다중 취소 문장 개수가 50개를 초과하면 실패한다`() {
        val exception =
            assertFailsWith<CustomException> {
                quoteScrapUseCase.deleteQuoteScraps(USER_ID, (1L..51L).toList())
            }

        assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
    }

    private fun scrappedQuote(quoteId: Long): ScrappedQuote =
        ScrappedQuote(
            quoteId = quoteId,
            bookId = BOOK_ID,
            bookCoverImageUrl = "https://cdn.example.com/book-cover.png",
            content = "새는 알에서 나오려고 투쟁한다.",
            title = "데미안",
            author = "헤르만 헤세",
            scrappedAt = SCRAPPED_AT,
        )

    private fun quoteNotFoundException(): CustomException = CustomException(ErrorCode.NOT_FOUND)

    private companion object {
        const val USER_ID = 1L
        const val QUOTE_ID = 10L
        const val BOOK_ID = 100L
        const val LIMIT = 10
        const val FETCH_LIMIT = 11
        const val TOTAL_COUNT = 3
        const val EXPECTED_NEXT_CURSOR = "MjAyNi0wNi0xM1QxNDozMDoxMHwxMA"
        val SCRAPPED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 13, 14, 30, 10)
    }
}
