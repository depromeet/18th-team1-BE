package com.firstpenguin.app.domain.recommendation.repository

import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RecommendationRepositoryTest {
    @Test
    fun `하루 추천 횟수 조회는 삭제된 추천도 포함한다`() {
        val capturedSql =
            captureCountSql { dsl ->
                RecommendationRepository(dsl).countByUserIdAndRecommendationDate(
                    userId = USER_ID,
                    recommendationDate = TODAY,
                )
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("from \"recommendations\""), normalizedSql)
        assertFalse(normalizedSql.contains("\"deleted_at\" is null"), normalizedSql)
    }

    @Test
    fun `추천 기록 기간 조회는 삭제된 추천을 제외한다`() {
        val capturedSql =
            captureEmptyResultSql { dsl ->
                RecommendationRepository(dsl).findCompletedRecommendationsByUserIdAndRecommendationDateBetween(
                    userId = USER_ID,
                    start = TODAY,
                    end = TODAY,
                )
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("\"recommendations\".\"quote_id\" is not null"), normalizedSql)
        assertTrue(normalizedSql.contains("\"recommendations\".\"deleted_at\" is null"), normalizedSql)
    }

    @Test
    fun `추천 상세 조회는 삭제된 추천을 제외한다`() {
        val capturedSql =
            captureEmptyResultSql { dsl ->
                RecommendationRepository(dsl).findRecommendationById(RECOMMENDATION_ID)
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("\"recommendations\".\"id\" = ?"), normalizedSql)
        assertTrue(normalizedSql.contains("\"recommendations\".\"deleted_at\" is null"), normalizedSql)
    }

    private fun captureCountSql(repositoryCall: (DSLContext) -> Unit): String {
        var capturedSql = ""
        lateinit var dsl: DSLContext
        val connection =
            MockConnection(
                MockDataProvider { context ->
                    capturedSql = context.sql()
                    arrayOf(MockResult(0, dsl.newResult()))
                },
            )
        dsl = DSL.using(connection, SQLDialect.POSTGRES)
        repositoryCall(dsl)
        return capturedSql
    }

    private fun captureEmptyResultSql(repositoryCall: (DSLContext) -> Unit): String {
        var capturedSql = ""
        lateinit var dsl: DSLContext
        val connection =
            MockConnection(
                MockDataProvider { context ->
                    capturedSql = context.sql()
                    arrayOf(MockResult(0, dsl.newResult()))
                },
            )
        dsl = DSL.using(connection, SQLDialect.POSTGRES)
        repositoryCall(dsl)
        return capturedSql
    }

    private companion object {
        const val USER_ID = 1L
        const val RECOMMENDATION_ID = 2L
        val TODAY: LocalDate = LocalDate.of(2026, 6, 19)
    }
}
