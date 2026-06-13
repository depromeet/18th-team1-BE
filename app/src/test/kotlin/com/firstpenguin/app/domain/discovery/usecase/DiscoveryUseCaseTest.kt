package com.firstpenguin.app.domain.discovery.usecase

import com.firstpenguin.app.domain.discovery.model.DiscoveryCursor
import com.firstpenguin.app.domain.discovery.model.DiscoveryGenre
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuote
import com.firstpenguin.app.domain.discovery.service.DiscoveryService
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

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
        Mockito
            .`when`(discoveryService.getRecommendedQuotes(USER_ID, null, null, DISCOVERY_QUOTE_FETCH_COUNT))
            .thenReturn(listOf(quote))

        val response = discoveryUseCase.getDiscoveryQuotes(USER_ID, cursor = null, genre = null)

        assertEquals(1, response.quotes.size)
        assertEquals(RECOMMENDED_USER_ID, response.quotes.first().recommendedUserId)
        assertEquals(RECOMMENDED_AT, response.quotes.first().recommendedAt)
        assertEquals(GENRE, response.quotes.first().genre)
        assertFalse(response.quotes.first().isScrapped)
        assertFalse(response.hasNext)
        assertNull(response.nextCursor)
    }

    @Test
    fun `조회된 스크랩 여부가 true면 응답도 true다`() {
        val quote = discoveryQuote(QUOTE_ID, isScrapped = true)
        Mockito
            .`when`(discoveryService.getRecommendedQuotes(USER_ID, null, null, DISCOVERY_QUOTE_FETCH_COUNT))
            .thenReturn(listOf(quote))

        val response = discoveryUseCase.getDiscoveryQuotes(USER_ID, cursor = null, genre = null)

        assertEquals(true, response.quotes.first().isScrapped)
    }

    @Test
    fun `조회 결과가 11개면 10개만 응답하고 다음 커서를 만든다`() {
        val quotes = (1L..DISCOVERY_QUOTE_FETCH_COUNT).map { quoteId -> discoveryQuote(quoteId) }
        Mockito
            .`when`(discoveryService.getRecommendedQuotes(USER_ID, null, null, DISCOVERY_QUOTE_FETCH_COUNT))
            .thenReturn(quotes)

        val response = discoveryUseCase.getDiscoveryQuotes(USER_ID, cursor = null, genre = null)

        assertEquals(DISCOVERY_QUOTE_COUNT, response.quotes.size)
        assertEquals(true, response.hasNext)
        assertEquals(EXPECTED_NEXT_CURSOR, response.nextCursor)
    }

    @Test
    fun `유효한 커서로 조회하면 파싱된 커서가 서비스에 전달된다`() {
        val quote = discoveryQuote(QUOTE_ID)
        Mockito
            .`when`(
                discoveryService.getRecommendedQuotes(
                    USER_ID,
                    DiscoveryCursor(RECOMMENDED_AT, QUOTE_ID),
                    null,
                    DISCOVERY_QUOTE_FETCH_COUNT,
                ),
            ).thenReturn(listOf(quote))

        val response = discoveryUseCase.getDiscoveryQuotes(USER_ID, cursor = EXPECTED_NEXT_CURSOR, genre = null)

        assertEquals(1, response.quotes.size)
    }

    @Test
    fun `장르가 전체면 전체 장르를 조회한다`() {
        Mockito
            .`when`(discoveryService.getRecommendedQuotes(USER_ID, null, null, DISCOVERY_QUOTE_FETCH_COUNT))
            .thenReturn(emptyList())

        val response = discoveryUseCase.getDiscoveryQuotes(USER_ID, cursor = null, genre = "전체")

        assertEquals(emptyList(), response.quotes)
        assertFalse(response.hasNext)
    }

    @Test
    fun `유효한 장르로 조회하면 파싱된 장르가 서비스에 전달된다`() {
        val quote = discoveryQuote(QUOTE_ID)
        Mockito
            .`when`(
                discoveryService.getRecommendedQuotes(
                    USER_ID,
                    null,
                    DiscoveryGenre.KOREAN_NOVEL,
                    DISCOVERY_QUOTE_FETCH_COUNT,
                ),
            ).thenReturn(listOf(quote))

        val response = discoveryUseCase.getDiscoveryQuotes(USER_ID, cursor = null, genre = GENRE)

        assertEquals(1, response.quotes.size)
    }

    @Test
    fun `유효한 커서와 장르로 조회하면 둘 다 파싱되어 서비스에 전달된다`() {
        val quote = discoveryQuote(QUOTE_ID)
        Mockito
            .`when`(
                discoveryService.getRecommendedQuotes(
                    USER_ID,
                    DiscoveryCursor(RECOMMENDED_AT, QUOTE_ID),
                    DiscoveryGenre.KOREAN_NOVEL,
                    DISCOVERY_QUOTE_FETCH_COUNT,
                ),
            ).thenReturn(listOf(quote))

        val response = discoveryUseCase.getDiscoveryQuotes(USER_ID, cursor = EXPECTED_NEXT_CURSOR, genre = GENRE)

        assertEquals(1, response.quotes.size)
    }

    @Test
    fun `유효하지 않은 장르가 들어오면 예외가 발생한다`() {
        val exception =
            org.junit.jupiter.api.assertThrows<CustomException> {
                discoveryUseCase.getDiscoveryQuotes(USER_ID, cursor = null, genre = "미지원")
            }

        assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
    }

    @Test
    fun `잘못된 커서가 들어오면 예외가 발생한다`() {
        val exception =
            org.junit.jupiter.api.assertThrows<CustomException> {
                discoveryUseCase.getDiscoveryQuotes(USER_ID, cursor = "invalid", genre = null)
            }

        assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
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
            genre = GENRE,
            recommendedAt = RECOMMENDED_AT,
            isScrapped = isScrapped,
        )

    private companion object {
        const val DISCOVERY_QUOTE_COUNT = 10
        const val DISCOVERY_QUOTE_FETCH_COUNT = 11
        const val USER_ID = 1L
        const val QUOTE_ID = 10L
        const val BOOK_ID = 100L
        const val RECOMMENDED_USER_ID = 200L
        const val GENRE = "한국소설"
        val RECOMMENDED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 5, 12, 34, 56)
        const val EXPECTED_NEXT_CURSOR = "MjAyNi0wNi0wNVQxMjozNDo1NnwxMA"
    }
}
