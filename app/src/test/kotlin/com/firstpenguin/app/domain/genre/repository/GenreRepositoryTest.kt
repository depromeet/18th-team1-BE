package com.firstpenguin.app.domain.genre.repository

import com.firstpenguin.app.domain.genre.repository.table.GenreTable
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class GenreRepositoryTest {
    @Test
    fun `장르 목록은 정렬 순서와 ID 기준으로 조회한다`() {
        val capturedSql =
            captureSql { dsl ->
                GenreRepository(dsl).findAll()
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("from \"genres\""), normalizedSql)
        assertTrue(
            normalizedSql.contains("order by \"genres\".\"sort_order\" asc, \"genres\".\"id\" asc"),
            normalizedSql,
        )
    }

    @Test
    fun `장르 ID 존재 여부를 조회한다`() {
        val capturedSql =
            captureSql { dsl ->
                GenreRepository(dsl).existsById(GENRE_ID)
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("from \"genres\""), normalizedSql)
        assertTrue(normalizedSql.contains("\"genres\".\"id\" = ?"), normalizedSql)
    }

    private fun captureSql(repositoryCall: (DSLContext) -> Unit): String {
        var capturedSql = ""
        lateinit var dsl: DSLContext
        val connection =
            MockConnection(
                MockDataProvider { context ->
                    capturedSql = context.sql()
                    arrayOf(resultFor(capturedSql, dsl))
                },
            )
        dsl = DSL.using(connection, SQLDialect.POSTGRES)
        repositoryCall(dsl)
        return capturedSql
    }

    private fun resultFor(
        sql: String,
        dsl: DSLContext,
    ): MockResult {
        if (sql.contains("exists")) return existsResult(dsl)

        return MockResult(0, dsl.newResult(GenreTable.ID, GenreTable.LABEL, GenreTable.SORT_ORDER))
    }

    private fun existsResult(dsl: DSLContext): MockResult {
        val existsField = DSL.field("exists", Boolean::class.java)
        val result = dsl.newResult(existsField)
        result.add(dsl.newRecord(existsField).apply { set(existsField, true) })
        return MockResult(1, result)
    }

    private companion object {
        const val GENRE_ID = 1L
    }
}
