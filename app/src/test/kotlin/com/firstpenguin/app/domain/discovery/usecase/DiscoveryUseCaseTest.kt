package com.firstpenguin.app.domain.discovery.usecase

import com.firstpenguin.app.domain.discovery.model.DiscoveryCursor
import com.firstpenguin.app.domain.discovery.model.DiscoveryGenre
import com.firstpenguin.app.domain.discovery.model.DiscoveryNeedTag
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuote
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuoteSearchCriteria
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuoteSearchCursor
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuoteSearchSort
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
        assertEquals(EMOTION_VALUE, response.quotes.first().emotionValue)
        assertEquals(EMOTION_LABEL, response.quotes.first().emotionLabel)
        assertEquals(GENRE, response.quotes.first().genre)
        assertEquals(
            NEED_TAG_ID,
            response.quotes
                .first()
                .needTag
                ?.id,
        )
        assertEquals(
            NEED_TAG_LABEL,
            response.quotes
                .first()
                .needTag
                ?.label,
        )
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

    @Test
    fun `검색어로 검색하면 기본 최신순으로 조회하고 발견탭 응답을 반환한다`() {
        val quote = discoveryQuote(QUOTE_ID)
        Mockito
            .`when`(
                discoveryService.searchRecommendedQuotes(
                    searchCriteria(),
                ),
            ).thenReturn(listOf(quote))

        val response =
            discoveryUseCase.searchDiscoveryQuotes(
                USER_ID,
                SEARCH_QUERY,
                sort = null,
                cursor = null,
                genre = null,
            )

        assertEquals(1, response.quotes.size)
        assertEquals(
            NEED_TAG_LABEL,
            response.quotes
                .first()
                .needTag
                ?.label,
        )
        assertFalse(response.hasNext)
    }

    @Test
    fun `스크랩순 검색은 스크랩수 커서를 파싱해 서비스에 전달한다`() {
        val quote = discoveryQuote(QUOTE_ID, scrapCount = SCRAP_COUNT)
        Mockito
            .`when`(
                discoveryService.searchRecommendedQuotes(
                    searchCriteria(
                        sort = DiscoveryQuoteSearchSort.SCRAP_COUNT,
                        cursor = DiscoveryQuoteSearchCursor(RECOMMENDED_AT, QUOTE_ID, SCRAP_COUNT),
                        genre = DiscoveryGenre.KOREAN_NOVEL,
                    ),
                ),
            ).thenReturn(listOf(quote))

        val response =
            discoveryUseCase.searchDiscoveryQuotes(
                USER_ID,
                SEARCH_QUERY,
                sort = "scrapCount",
                cursor = EXPECTED_SCRAP_COUNT_CURSOR,
                genre = GENRE,
            )

        assertEquals(1, response.quotes.size)
    }

    @Test
    fun `검색 결과가 11개면 정렬 기준에 맞는 다음 커서를 만든다`() {
        val quotes =
            (1L..DISCOVERY_QUOTE_FETCH_COUNT).map { quoteId ->
                discoveryQuote(quoteId, scrapCount = SCRAP_COUNT)
            }
        Mockito
            .`when`(
                discoveryService.searchRecommendedQuotes(
                    searchCriteria(sort = DiscoveryQuoteSearchSort.SCRAP_COUNT),
                ),
            ).thenReturn(quotes)

        val response =
            discoveryUseCase.searchDiscoveryQuotes(
                USER_ID,
                SEARCH_QUERY,
                sort = "scrapCount",
                cursor = null,
                genre = null,
            )

        assertEquals(DISCOVERY_QUOTE_COUNT, response.quotes.size)
        assertEquals(EXPECTED_SCRAP_COUNT_CURSOR, response.nextCursor)
        assertEquals(true, response.hasNext)
    }

    @Test
    fun `빈 검색어가 들어오면 예외가 발생한다`() {
        val exception =
            org.junit.jupiter.api.assertThrows<CustomException> {
                discoveryUseCase.searchDiscoveryQuotes(
                    USER_ID,
                    query = "  ",
                    sort = null,
                    cursor = null,
                    genre = null,
                )
            }

        assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
    }

    @Test
    fun `유효하지 않은 검색 정렬이 들어오면 예외가 발생한다`() {
        val exception =
            org.junit.jupiter.api.assertThrows<CustomException> {
                discoveryUseCase.searchDiscoveryQuotes(
                    USER_ID,
                    SEARCH_QUERY,
                    sort = "oldest",
                    cursor = null,
                    genre = null,
                )
            }

        assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
    }

    private fun discoveryQuote(
        quoteId: Long,
        isScrapped: Boolean = false,
        scrapCount: Int = 0,
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
            needTag = DiscoveryNeedTag(NEED_TAG_ID, NEED_TAG_LABEL),
            emotionValue = EMOTION_VALUE,
            recommendedAt = RECOMMENDED_AT,
            isScrapped = isScrapped,
            scrapCount = scrapCount,
        )

    private fun searchCriteria(
        sort: DiscoveryQuoteSearchSort = DiscoveryQuoteSearchSort.LATEST,
        cursor: DiscoveryQuoteSearchCursor? = null,
        genre: DiscoveryGenre? = null,
    ): DiscoveryQuoteSearchCriteria =
        DiscoveryQuoteSearchCriteria(
            userId = USER_ID,
            query = SEARCH_QUERY,
            sort = sort,
            cursor = cursor,
            genre = genre,
            limit = DISCOVERY_QUOTE_FETCH_COUNT,
        )

    private companion object {
        const val DISCOVERY_QUOTE_COUNT = 10
        const val DISCOVERY_QUOTE_FETCH_COUNT = 11
        const val USER_ID = 1L
        const val QUOTE_ID = 10L
        const val BOOK_ID = 100L
        const val RECOMMENDED_USER_ID = 200L
        const val NEED_TAG_ID = 49L
        const val NEED_TAG_LABEL = "공감해주는 문장"
        const val GENRE = "한국소설"
        const val EMOTION_VALUE = 7
        const val EMOTION_LABEL = "약간 기분 좋아요"
        const val SEARCH_QUERY = "새"
        const val SCRAP_COUNT = 3
        val RECOMMENDED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 5, 12, 34, 56)
        const val EXPECTED_NEXT_CURSOR = "MjAyNi0wNi0wNVQxMjozNDo1NnwxMA"
        const val EXPECTED_SCRAP_COUNT_CURSOR = "M3wyMDI2LTA2LTA1VDEyOjM0OjU2fDEw"
    }
}
