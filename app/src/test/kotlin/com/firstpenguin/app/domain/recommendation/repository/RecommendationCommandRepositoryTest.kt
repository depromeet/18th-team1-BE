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

class RecommendationCommandRepositoryTest {
    @Test
    fun `추천 삭제는 hard delete가 아니라 deleted_at을 갱신한다`() {
        val capturedSql =
            captureSql { dsl ->
                RecommendationCommandRepository(dsl).softDeleteById(RECOMMENDATION_ID)
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.startsWith("update \"recommendations\""), normalizedSql)
        assertTrue(normalizedSql.contains("\"deleted_at\""), normalizedSql)
        assertTrue(normalizedSql.contains("\"recommendations\".\"id\" = ?"), normalizedSql)
        assertTrue(normalizedSql.contains("\"recommendations\".\"deleted_at\" is null"), normalizedSql)
        assertFalse(normalizedSql.contains("delete from"), normalizedSql)
    }

    private fun captureSql(repositoryCall: (DSLContext) -> Unit): String {
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
        const val RECOMMENDATION_ID = 1L
    }
}
