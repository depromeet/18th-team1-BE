package com.firstpenguin.app.domain.quote.service

import com.firstpenguin.app.domain.quote.model.QuoteScrapCursor
import com.firstpenguin.app.domain.quote.model.ScrappedQuote
import com.firstpenguin.app.domain.quote.repository.QuoteScrapRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime
import kotlin.test.assertEquals

class QuoteScrapServiceTest {
    private lateinit var quoteScrapRepository: QuoteScrapRepository
    private lateinit var quoteScrapService: QuoteScrapService

    @BeforeEach
    fun setUp() {
        quoteScrapRepository = Mockito.mock(QuoteScrapRepository::class.java)
        quoteScrapService = QuoteScrapService(quoteScrapRepository)
    }

    @Test
    fun `스크랩 상태로 설정한다`() {
        Mockito.`when`(quoteScrapRepository.insertIgnoreDuplicate(USER_ID, QUOTE_ID)).thenReturn(1)

        quoteScrapService.setQuoteScrap(USER_ID, QUOTE_ID)

        Mockito.verify(quoteScrapRepository).insertIgnoreDuplicate(USER_ID, QUOTE_ID)
    }

    @Test
    fun `이미 스크랩한 문장 상태 설정 요청도 성공 처리한다`() {
        Mockito.`when`(quoteScrapRepository.insertIgnoreDuplicate(USER_ID, QUOTE_ID)).thenReturn(0)

        quoteScrapService.setQuoteScrap(USER_ID, QUOTE_ID)

        Mockito.verify(quoteScrapRepository).insertIgnoreDuplicate(USER_ID, QUOTE_ID)
    }

    @Test
    fun `스크랩을 취소한다`() {
        Mockito.`when`(quoteScrapRepository.deleteByUserIdAndQuoteId(USER_ID, QUOTE_ID)).thenReturn(1)

        quoteScrapService.deleteQuoteScrap(USER_ID, QUOTE_ID)

        Mockito.verify(quoteScrapRepository).deleteByUserIdAndQuoteId(USER_ID, QUOTE_ID)
    }

    @Test
    fun `존재하지 않는 스크랩 취소 요청도 성공 처리한다`() {
        Mockito.`when`(quoteScrapRepository.deleteByUserIdAndQuoteId(USER_ID, QUOTE_ID)).thenReturn(0)

        quoteScrapService.deleteQuoteScrap(USER_ID, QUOTE_ID)

        Mockito.verify(quoteScrapRepository).deleteByUserIdAndQuoteId(USER_ID, QUOTE_ID)
    }

    @Test
    fun `여러 스크랩을 취소한다`() {
        val quoteIds = listOf(10L, 20L)

        quoteScrapService.deleteQuoteScraps(USER_ID, quoteIds)

        Mockito.verify(quoteScrapRepository).deleteByUserIdAndQuoteIds(USER_ID, quoteIds)
    }

    @Test
    fun `스크랩 목록을 조회한다`() {
        val cursor = QuoteScrapCursor(SCRAPPED_AT, QUOTE_ID)
        val quotes = listOf(scrappedQuote())
        Mockito.`when`(quoteScrapRepository.findActiveByUserId(USER_ID, cursor, LIMIT)).thenReturn(quotes)

        val response = quoteScrapService.getScrappedQuotes(USER_ID, cursor, LIMIT)

        assertEquals(quotes, response)
    }

    @Test
    fun `스크랩 문장 개수를 조회한다`() {
        Mockito.`when`(quoteScrapRepository.countActiveByUserId(USER_ID)).thenReturn(TOTAL_COUNT)

        val count = quoteScrapService.countQuoteScraps(USER_ID)

        assertEquals(TOTAL_COUNT, count)
    }

    private fun scrappedQuote(): ScrappedQuote =
        ScrappedQuote(
            quoteId = QUOTE_ID,
            bookId = BOOK_ID,
            bookCoverImageUrl = "https://cdn.example.com/book-cover.png",
            content = "새는 알에서 나오려고 투쟁한다.",
            title = "데미안",
            author = "헤르만 헤세",
            scrappedAt = SCRAPPED_AT,
        )

    private companion object {
        const val USER_ID = 1L
        const val QUOTE_ID = 10L
        const val BOOK_ID = 100L
        const val LIMIT = 10
        const val TOTAL_COUNT = 3
        val SCRAPPED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 13, 14, 30)
    }
}
