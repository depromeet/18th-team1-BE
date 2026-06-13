package com.firstpenguin.app.domain.quote.repository

import com.firstpenguin.app.domain.book.repository.BookTable
import com.firstpenguin.app.domain.quote.model.QuoteScrapCursor
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

class QuoteScrapRepositoryTest {
    @Test
    fun `중복 스크랩 생성을 무시하는 쿼리를 실행한다`() {
        val capturedSql =
            captureSql { dsl ->
                QuoteScrapRepository(dsl).insertIgnoreDuplicate(USER_ID, QUOTE_ID)
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("insert into \"quote_scraps\""), normalizedSql)
        assertTrue(normalizedSql.contains("\"user_id\""), normalizedSql)
        assertTrue(normalizedSql.contains("\"quote_id\""), normalizedSql)
        assertTrue(normalizedSql.contains("on conflict"), normalizedSql)
        assertTrue(normalizedSql.contains("do nothing"), normalizedSql)
    }

    @Test
    fun `사용자와 문장 기준으로 스크랩을 삭제한다`() {
        val capturedSql =
            captureSql { dsl ->
                QuoteScrapRepository(dsl).deleteByUserIdAndQuoteId(USER_ID, QUOTE_ID)
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("delete from \"quote_scraps\""), normalizedSql)
        assertTrue(normalizedSql.contains("\"quote_scraps\".\"user_id\" = ?"), normalizedSql)
        assertTrue(normalizedSql.contains("\"quote_scraps\".\"quote_id\" = ?"), normalizedSql)
    }

    @Test
    fun `여러 문장 기준으로 스크랩을 삭제한다`() {
        val capturedSql =
            captureSql { dsl ->
                QuoteScrapRepository(dsl).deleteByUserIdAndQuoteIds(USER_ID, listOf(QUOTE_ID, 20L))
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("delete from \"quote_scraps\""), normalizedSql)
        assertTrue(normalizedSql.contains("\"quote_scraps\".\"user_id\" = ?"), normalizedSql)
        assertTrue(normalizedSql.contains("\"quote_scraps\".\"quote_id\" in"), normalizedSql)
    }

    @Test
    fun `스크랩 목록을 최신순으로 조회한다`() {
        val capturedSql =
            captureScrappedQuoteSql { dsl ->
                QuoteScrapRepository(dsl).findActiveByUserId(USER_ID, null, LIMIT)
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("from \"quote_scraps\""), normalizedSql)
        assertTrue(normalizedSql.contains("join \"quotes\""), normalizedSql)
        assertTrue(normalizedSql.contains("join \"books\""), normalizedSql)
        assertTrue(normalizedSql.contains("\"quote_scraps\".\"user_id\" = ?"), normalizedSql)
        assertTrue(normalizedSql.contains("\"quotes\".\"deleted_at\" is null"), normalizedSql)
        assertTrue(normalizedSql.contains("\"books\".\"deleted_at\" is null"), normalizedSql)
        assertTrue(normalizedSql.contains("order by \"quote_scraps\".\"created_at\" desc"), normalizedSql)
        assertTrue(normalizedSql.contains("\"quote_scraps\".\"quote_id\" desc"), normalizedSql)
        assertTrue(normalizedSql.contains("fetch next ? rows only"), normalizedSql)
    }

    @Test
    fun `커서가 있으면 스크랩 목록을 다음 페이지 조건으로 조회한다`() {
        val capturedSql =
            captureScrappedQuoteSql { dsl ->
                QuoteScrapRepository(dsl).findActiveByUserId(
                    userId = USER_ID,
                    cursor = QuoteScrapCursor(SCRAPPED_AT, QUOTE_ID),
                    limit = LIMIT,
                )
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("\"quote_scraps\".\"created_at\" <"), normalizedSql)
        assertTrue(normalizedSql.contains("\"quote_scraps\".\"created_at\" ="), normalizedSql)
        assertTrue(normalizedSql.contains("\"quote_scraps\".\"quote_id\" < ?"), normalizedSql)
    }

    private fun captureSql(repositoryCall: (DSLContext) -> Unit): String {
        var capturedSql = ""
        val connection =
            MockConnection(
                MockDataProvider { context ->
                    capturedSql = context.sql()
                    arrayOf(MockResult(1))
                },
            )
        repositoryCall(DSL.using(connection, SQLDialect.POSTGRES))
        return capturedSql
    }

    private fun captureScrappedQuoteSql(repositoryCall: (DSLContext) -> Unit): String {
        var capturedSql = ""
        lateinit var dsl: DSLContext
        val connection =
            MockConnection(
                MockDataProvider { context ->
                    capturedSql = context.sql()
                    arrayOf(MockResult(0, dsl.newResult(*SCRAPPED_QUOTE_FIELDS)))
                },
            )
        dsl = DSL.using(connection, SQLDialect.POSTGRES)
        repositoryCall(dsl)
        return capturedSql
    }

    private companion object {
        const val USER_ID = 1L
        const val QUOTE_ID = 10L
        const val LIMIT = 10
        val SCRAPPED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 13, 14, 30)
        val SCRAPPED_QUOTE_FIELDS: Array<Field<*>> =
            arrayOf(
                QuoteTable.ID,
                BookTable.ID,
                BookTable.COVER_IMAGE_URL,
                QuoteTable.CONTENT,
                BookTable.TITLE,
                BookTable.AUTHOR,
                QuoteScrapTable.CREATED_AT,
            )
    }
}
