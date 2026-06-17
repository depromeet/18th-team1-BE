package com.firstpenguin.app.domain.discovery.repository

import com.firstpenguin.app.domain.book.repository.BookTable
import com.firstpenguin.app.domain.discovery.model.DiscoveryCursor
import com.firstpenguin.app.domain.discovery.model.DiscoveryGenre
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuoteSearchCriteria
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuoteSearchCursor
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuoteSearchSort
import com.firstpenguin.app.domain.quote.repository.QuoteScrapTable
import com.firstpenguin.app.domain.quote.repository.QuoteTable
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiscoveryRepositoryTest {
    @Test
    fun `추천 이력 테이블을 합쳐 최신순 문장을 조회한다`() {
        val capturedSql =
            captureSql { dsl ->
                DiscoveryRepository(dsl).findRecommendedQuotes(
                    userId = USER_ID,
                    cursor = null,
                    genre = null,
                    limit = DISCOVERY_QUOTE_FETCH_COUNT,
                )
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("recommendations"), normalizedSql)
        assertTrue(normalizedSql.contains("recommendation_quotes"), normalizedSql)
        assertFalse(normalizedSql.contains("daily_recommendation_quotes"), normalizedSql)
        assertFalse(normalizedSql.contains("daily_recommendation_id"), normalizedSql)
        assertTrue(normalizedSql.contains("recommended_user_id"), normalizedSql)
        assertTrue(normalizedSql.contains("recommended_at"), normalizedSql)
        assertTrue(normalizedSql.contains("quote_scraps"), normalizedSql)
        assertTrue(normalizedSql.contains("is_scrapped"), normalizedSql)
        assertTrue(normalizedSql.contains("recommendation_tags"), normalizedSql)
        assertTrue(normalizedSql.contains("need_tag_id"), normalizedSql)
        assertTrue(normalizedSql.contains("need_tag_label"), normalizedSql)
        assertTrue(normalizedSql.contains("\"tags\".\"type\" = ?"), normalizedSql)
        assertTrue(normalizedSql.contains("\"quote_scraps\".\"user_id\" = ?"), normalizedSql)
        assertTrue(normalizedSql.contains("row_number()"), normalizedSql)
        assertTrue(normalizedSql.contains(" union all "), normalizedSql)
        assertTrue(
            normalizedSql.contains(
                "order by \"ranked_recommendation_events\".\"recommended_at\" desc, \"quotes\".\"id\" desc",
            ),
            normalizedSql,
        )
        assertTrue(normalizedSql.contains("fetch next ? rows only"), normalizedSql)
        assertTrue(normalizedSql.contains("\"recommendation_rank\" = ?"), normalizedSql)
        assertTrue(normalizedSql.contains("\"quotes\".\"deleted_at\" is null"), normalizedSql)
        assertTrue(normalizedSql.contains("\"books\".\"deleted_at\" is null"), normalizedSql)
    }

    @Test
    fun `커서가 있으면 다음 페이지 조건을 추가한다`() {
        val capturedSql =
            captureSql { dsl ->
                DiscoveryRepository(dsl).findRecommendedQuotes(
                    userId = USER_ID,
                    cursor = DiscoveryCursor(RECOMMENDED_AT, QUOTE_ID),
                    genre = null,
                    limit = DISCOVERY_QUOTE_FETCH_COUNT,
                )
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("\"recommended_at\" < cast(? as timestamp)"), normalizedSql)
        assertTrue(normalizedSql.contains("\"recommended_at\" = cast(? as timestamp)"), normalizedSql)
        assertTrue(normalizedSql.contains("\"quotes\".\"id\" < ?"), normalizedSql)
    }

    @Test
    fun `장르가 있으면 책 카테고리 조건을 추가한다`() {
        val capturedSql =
            captureSql { dsl ->
                DiscoveryRepository(dsl).findRecommendedQuotes(
                    userId = USER_ID,
                    cursor = null,
                    genre = DiscoveryGenre.KOREAN_NOVEL,
                    limit = DISCOVERY_QUOTE_FETCH_COUNT,
                )
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("\"books\".\"category\" = ?"), normalizedSql)
    }

    @Test
    fun `검색어가 있으면 문장 내용 검색 조건을 추가한다`() {
        val capturedSql =
            captureSql { dsl ->
                DiscoveryRepository(dsl).searchRecommendedQuotes(
                    searchCriteria(),
                )
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("\"quotes\".\"content\" ilike"), normalizedSql)
        assertFalse(normalizedSql.contains("\"books\".\"title\" ilike"), normalizedSql)
        assertFalse(normalizedSql.contains("\"books\".\"author\" ilike"), normalizedSql)
        assertFalse(normalizedSql.contains("\"users\".\"nickname\" ilike"), normalizedSql)
        assertTrue(
            normalizedSql.contains(
                "order by \"ranked_recommendation_events\".\"recommended_at\" desc, \"quotes\".\"id\" desc",
            ),
            normalizedSql,
        )
    }

    @Test
    fun `스크랩순 검색은 스크랩 수 집계와 정렬을 추가한다`() {
        val capturedSql =
            captureSql { dsl ->
                DiscoveryRepository(dsl).searchRecommendedQuotes(
                    searchCriteria(
                        sort = DiscoveryQuoteSearchSort.SCRAP_COUNT,
                        genre = DiscoveryGenre.KOREAN_NOVEL,
                    ),
                )
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("quote_scrap_counts"), normalizedSql)
        assertTrue(normalizedSql.contains("count(\"quote_scraps\".\"id\")"), normalizedSql)
        assertTrue(normalizedSql.contains("\"books\".\"category\" = ?"), normalizedSql)
        assertTrue(normalizedSql.contains("order by \"scrap_count\" desc"), normalizedSql)
    }

    @Test
    fun `스크랩순 검색 커서가 있으면 스크랩 수와 최신순 보조 조건을 추가한다`() {
        val capturedSql =
            captureSql { dsl ->
                DiscoveryRepository(dsl).searchRecommendedQuotes(
                    searchCriteria(
                        sort = DiscoveryQuoteSearchSort.SCRAP_COUNT,
                        cursor = DiscoveryQuoteSearchCursor(RECOMMENDED_AT, QUOTE_ID, SCRAP_COUNT),
                    ),
                )
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("\"scrap_count\" < ?"), normalizedSql)
        assertTrue(normalizedSql.contains("\"scrap_count\" = ?"), normalizedSql)
        assertTrue(normalizedSql.contains("\"recommended_at\" < cast(? as timestamp)"), normalizedSql)
        assertTrue(normalizedSql.contains("\"quotes\".\"id\" < ?"), normalizedSql)
    }

    private fun captureSql(repositoryCall: (DSLContext) -> Unit): String {
        var capturedSql = ""
        lateinit var dsl: DSLContext
        val connection =
            MockConnection(
                MockDataProvider { context ->
                    capturedSql = context.sql()
                    arrayOf(MockResult(0, dsl.newResult(*DISCOVERY_QUOTE_FIELDS)))
                },
            )
        dsl = DSL.using(connection, SQLDialect.POSTGRES)
        repositoryCall(dsl)
        return capturedSql
    }

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
        const val USER_ID = 1L
        const val DISCOVERY_QUOTE_FETCH_COUNT = 11
        const val QUOTE_ID = 10L
        const val SEARCH_QUERY = "새"
        const val SCRAP_COUNT = 3
        val RECOMMENDED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 5, 12, 34, 56)
        val DISCOVERY_QUOTE_FIELDS: Array<Field<*>> =
            arrayOf(
                QuoteTable.ID,
                BookTable.ID,
                DSL.field("recommended_user_id", Long::class.java),
                QuoteTable.CONTENT,
                BookTable.TITLE,
                BookTable.AUTHOR,
                BookTable.COVER_IMAGE_URL,
                BookTable.CATEGORY,
                DSL.field("need_tag_id", Long::class.java),
                DSL.field("need_tag_label", String::class.java),
                DSL.field("recommended_at", LocalDateTime::class.java),
                QuoteScrapTable.ID.isNotNull.`as`("is_scrapped"),
                DSL.field("scrap_count", Int::class.java),
            )
    }
}
