package com.firstpenguin.app.domain.discovery.repository

import com.firstpenguin.app.domain.book.repository.BookTable
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
import kotlin.test.assertTrue

class DiscoveryRepositoryTest {
    @Test
    fun `추천 이력 테이블을 합쳐 랜덤 문장을 조회한다`() {
        val capturedSql =
            captureSql { dsl ->
                DiscoveryRepository(dsl).findRandomRecommendedQuotes(USER_ID, DISCOVERY_QUOTE_COUNT)
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("daily_recommendations"), normalizedSql)
        assertTrue(normalizedSql.contains("daily_recommendation_quotes"), normalizedSql)
        assertTrue(normalizedSql.contains("recommended_user_id"), normalizedSql)
        assertTrue(normalizedSql.contains("recommended_at"), normalizedSql)
        assertTrue(normalizedSql.contains("quote_scraps"), normalizedSql)
        assertTrue(normalizedSql.contains("is_scrapped"), normalizedSql)
        assertTrue(normalizedSql.contains("\"quote_scraps\".\"user_id\" = ?"), normalizedSql)
        assertTrue(normalizedSql.contains("row_number()"), normalizedSql)
        assertTrue(normalizedSql.contains(" union all "), normalizedSql)
        assertTrue(normalizedSql.contains("random()"), normalizedSql)
        assertTrue(normalizedSql.contains("fetch next ? rows only"), normalizedSql)
        assertTrue(normalizedSql.contains("\"recommendation_rank\" = ?"), normalizedSql)
        assertTrue(normalizedSql.contains("\"quotes\".\"deleted_at\" is null"), normalizedSql)
        assertTrue(normalizedSql.contains("\"books\".\"deleted_at\" is null"), normalizedSql)
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

    private companion object {
        const val USER_ID = 1L
        const val DISCOVERY_QUOTE_COUNT = 10
        val DISCOVERY_QUOTE_FIELDS: Array<Field<*>> =
            arrayOf(
                QuoteTable.ID,
                BookTable.ID,
                DSL.field("recommended_user_id", Long::class.java),
                QuoteTable.CONTENT,
                BookTable.TITLE,
                BookTable.AUTHOR,
                BookTable.COVER_IMAGE_URL,
                DSL.field("recommended_at", LocalDateTime::class.java),
                QuoteScrapTable.ID.isNotNull.`as`("is_scrapped"),
            )
    }
}
