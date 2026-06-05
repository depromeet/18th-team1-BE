package com.firstpenguin.app.domain.discovery.usecase

import com.firstpenguin.app.domain.discovery.model.DiscoveryQuote
import com.firstpenguin.app.domain.discovery.service.DiscoveryService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DiscoveryUseCaseTest {
    private lateinit var discoveryService: DiscoveryService
    private lateinit var discoveryUseCase: DiscoveryUseCase

    @BeforeEach
    fun setUp() {
        discoveryService = Mockito.mock(DiscoveryService::class.java)
        discoveryUseCase = DiscoveryUseCase(discoveryService)
    }

    @Test
    fun `로그인 사용자의 추천 이력 정보와 스크랩 여부를 응답한다`() {
        val quote = discoveryQuote(QUOTE_ID)
        Mockito.`when`(discoveryService.getRandomQuotes(USER_ID, DISCOVERY_QUOTE_COUNT)).thenReturn(listOf(quote))

        val response = discoveryUseCase.getDiscoveryQuotes(USER_ID)

        assertEquals(1, response.quotes.size)
        assertEquals(RECOMMENDED_USER_ID, response.quotes.first().recommendedUserId)
        assertEquals(RECOMMENDED_AT, response.quotes.first().recommendedAt)
        assertFalse(response.quotes.first().isScrapped)
    }

    @Test
    fun `조회된 스크랩 여부가 true면 응답도 true다`() {
        val quote = discoveryQuote(QUOTE_ID, isScrapped = true)
        Mockito.`when`(discoveryService.getRandomQuotes(USER_ID, DISCOVERY_QUOTE_COUNT)).thenReturn(listOf(quote))

        val response = discoveryUseCase.getDiscoveryQuotes(USER_ID)

        assertEquals(true, response.quotes.first().isScrapped)
    }

    private fun discoveryQuote(
        quoteId: Long,
        isScrapped: Boolean = false,
    ): DiscoveryQuote =
        DiscoveryQuote(
            quoteId = quoteId,
            bookId = BOOK_ID,
            recommendedUserId = RECOMMENDED_USER_ID,
            content = "새는 알에서 나오려고 투쟁한다.",
            title = "데미안",
            author = "헤르만 헤세",
            bookCoverImageUrl = "https://cdn.example.com/book-cover-placeholder.png",
            recommendedAt = RECOMMENDED_AT,
            isScrapped = isScrapped,
        )

    private companion object {
        const val DISCOVERY_QUOTE_COUNT = 10
        const val USER_ID = 1L
        const val QUOTE_ID = 10L
        const val BOOK_ID = 100L
        const val RECOMMENDED_USER_ID = 200L
        val RECOMMENDED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 5, 12, 34, 56)
    }
}
