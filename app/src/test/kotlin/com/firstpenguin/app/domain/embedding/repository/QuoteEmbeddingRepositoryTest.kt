package com.firstpenguin.app.domain.embedding.repository

import com.firstpenguin.app.domain.embedding.repository.table.QuoteEmbeddingTable
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QuoteEmbeddingRepositoryTest {
    @Test
    fun `quote embedding cosine similarity를 pgvector로 조회한다`() {
        val capturedSql =
            captureSql { dsl ->
                QuoteEmbeddingRepository(dsl).findCosineSimilarities(
                    quoteIds = listOf(1L, 2L),
                    userEmbedding = USER_EMBEDDING,
                )
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("1 -"), normalizedSql)
        assertTrue(normalizedSql.contains("<=>"), normalizedSql)
        assertTrue(normalizedSql.contains("?::vector(1536)"), normalizedSql)
        assertTrue(normalizedSql.contains("\"quote_embeddings\".\"quote_id\" in"), normalizedSql)
        assertTrue(normalizedSql.contains("\"quote_embeddings\".\"embedding_model\" = ?"), normalizedSql)
    }

    @Test
    fun `semantic fallback용 유사 quoteId를 조회한다`() {
        val capturedSql =
            captureSql { dsl ->
                QuoteEmbeddingRepository(dsl).findMostSimilarQuoteIds(
                    userEmbedding = USER_EMBEDDING,
                    excludedQuoteIds = listOf(1L),
                    limit = LIMIT,
                )
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("<=>"), normalizedSql)
        assertTrue(normalizedSql.contains("\"quote_embeddings\".\"quote_id\" not in"), normalizedSql)
        assertTrue(normalizedSql.contains("order by 1 -"), normalizedSql)
        assertTrue(normalizedSql.contains("fetch next ? rows only"), normalizedSql)
    }

    @Test
    fun `quoteId가 없으면 similarity를 조회하지 않는다`() {
        var queryCount = 0
        val connection =
            MockConnection(
                MockDataProvider {
                    queryCount += 1
                    arrayOf(MockResult(0, DSL.using(SQLDialect.POSTGRES).newResult(*RESULT_FIELDS)))
                },
            )
        val dsl = DSL.using(connection, SQLDialect.POSTGRES)

        val result =
            QuoteEmbeddingRepository(dsl).findCosineSimilarities(
                quoteIds = emptyList(),
                userEmbedding = USER_EMBEDDING,
            )

        assertEquals(emptyMap<Long, Double>(), result)
        assertEquals(0, queryCount)
    }

    private fun captureSql(repositoryCall: (DSLContext) -> Unit): String {
        var capturedSql = ""
        lateinit var dsl: DSLContext
        val connection =
            MockConnection(
                MockDataProvider { context ->
                    capturedSql = context.sql()
                    arrayOf(MockResult(0, dsl.newResult(*RESULT_FIELDS)))
                },
            )
        dsl = DSL.using(connection, SQLDialect.POSTGRES)
        repositoryCall(dsl)
        return capturedSql
    }

    private companion object {
        const val LIMIT = 300
        const val EMBEDDING_DIMENSION = 1536
        val USER_EMBEDDING: List<Double> = List(EMBEDDING_DIMENSION) { 0.1 }
        val SEMANTIC_SCORE_FIELD: Field<Double> =
            DSL.field(DSL.name("semantic_score"), Double::class.java)
        val RESULT_FIELDS: Array<Field<*>> =
            arrayOf(
                QuoteEmbeddingTable.QUOTE_ID,
                SEMANTIC_SCORE_FIELD,
            )
    }
}
