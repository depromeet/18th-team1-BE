package com.firstpenguin.app.domain.recommendation.repository

import com.firstpenguin.app.domain.book.repository.BookTable
import com.firstpenguin.app.domain.emotion.repository.table.TagTable
import com.firstpenguin.app.domain.quote.repository.QuoteTable
import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.global.enums.TagType
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.Result
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecommendationCandidateRepositoryTest {
    @Test
    fun `메타데이터 태그 기반 후보를 조회한다`() {
        val capturedQuery =
            captureQuery { dsl ->
                RecommendationCandidateRepository(dsl).findCandidates(
                    effectiveTags = listOf(EFFECTIVE_EMOTION_TAG, EFFECTIVE_NEED_TAG),
                    limit = LIMIT,
                )
            }
        val normalizedSql = capturedQuery.sql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("as \"candidate_quote_ids\""), normalizedSql)
        assertTrue(normalizedSql.contains("join \"quote_metadata\""), normalizedSql)
        assertTrue(normalizedSql.contains("join \"quote_metadata_tags\""), normalizedSql)
        assertTrue(normalizedSql.contains("join \"tags\""), normalizedSql)
        assertTrue(normalizedSql.contains("\"quotes\".\"deleted_at\" is null"), normalizedSql)
        assertTrue(normalizedSql.contains("\"books\".\"deleted_at\" is null"), normalizedSql)
        assertTrue(normalizedSql.contains("\"tags\".\"id\" in"), normalizedSql)
        assertTrue(normalizedSql.contains("\"tags\".\"type\" in"), normalizedSql)
        assertTrue(normalizedSql.contains("\"tags\".\"is_active\" = true"), normalizedSql)
        assertTrue(normalizedSql.contains("fetch next ? rows only"), normalizedSql)
        assertTrue(capturedQuery.bindings.contains(EMOTION_TAG_ID))
        assertFalse(capturedQuery.bindings.contains(NEED_TAG_ID))
        assertTrue(capturedQuery.bindings.contains(TagType.EMOTION.name))
        assertFalse(capturedQuery.bindings.contains(TagType.NEED.name))
    }

    @Test
    fun `emotion tag가 없으면 need tag로 fallback 후보를 조회한다`() {
        val capturedQuery =
            captureQuery { dsl ->
                RecommendationCandidateRepository(dsl).findCandidates(
                    effectiveTags = listOf(EFFECTIVE_NEED_TAG),
                    limit = LIMIT,
                )
            }

        assertTrue(capturedQuery.bindings.contains(NEED_TAG_ID))
        assertTrue(capturedQuery.bindings.contains(TagType.NEED.name))
    }

    @Test
    fun `조회 row를 후보 단위로 묶어 태그 타입별 id를 채운다`() {
        val candidates =
            findCandidatesWithResult { dsl ->
                candidateResult(dsl)
            }
        val candidate = candidates.first()

        assertEquals(1, candidates.size)
        assertEquals(QUOTE_ID, candidate.quoteId)
        assertEquals(BOOK_ID, candidate.bookId)
        assertEquals(QUOTE_CONTENT, candidate.content)
        assertEquals(BOOK_TITLE, candidate.title)
        assertEquals(BOOK_AUTHOR, candidate.author)
        assertEquals(ROLE_TAG_ID, candidate.roleTagId)
        assertEquals(setOf(EMOTION_TAG_ID), candidate.tagIdsByType[TagType.EMOTION])
        assertEquals(setOf(NEED_TAG_ID), candidate.tagIdsByType[TagType.NEED])
        assertEquals(setOf(MOOD_TAG_ID), candidate.tagIdsByType[TagType.MOOD])
        assertEquals(setOf(ROLE_TAG_ID), candidate.tagIdsByType[TagType.ROLE])
    }

    @Test
    fun `emotion need hard filter tag가 없으면 후보를 조회하지 않는다`() {
        val capturedSql =
            captureSql { dsl ->
                RecommendationCandidateRepository(dsl).findCandidates(
                    effectiveTags = listOf(EFFECTIVE_CONTEXT_TAG),
                    limit = LIMIT,
                )
            }

        assertEquals("", capturedSql)
    }

    @Test
    fun `완화 후보 조회는 hard filter 없이 metadata 후보를 조회한다`() {
        val capturedSql =
            captureSql { dsl ->
                RecommendationCandidateRepository(dsl).findRelaxedCandidates(limit = LIMIT)
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("join \"quote_metadata\""), normalizedSql)
        assertTrue(normalizedSql.contains("\"quotes\".\"deleted_at\" is null"), normalizedSql)
        assertTrue(normalizedSql.contains("\"books\".\"deleted_at\" is null"), normalizedSql)
        assertFalse(normalizedSql.contains("\"tags\".\"id\" in"), normalizedSql)
    }

    @Test
    fun `랜덤 후보 조회는 quote 후보를 무작위로 조회한다`() {
        val capturedSql =
            captureSql { dsl ->
                RecommendationCandidateRepository(dsl).findRandomCandidates(limit = LIMIT)
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("join \"quote_metadata\""), normalizedSql)
        assertTrue(
            normalizedSql.contains("order by rand()") || normalizedSql.contains("order by random()"),
            normalizedSql,
        )
    }

    @Test
    fun `태그가 없는 랜덤 후보도 조회 결과로 변환한다`() {
        val candidates =
            withMockedResult({ dsl -> noTagCandidateResult(dsl) }) { dsl ->
                RecommendationCandidateRepository(dsl).findRandomCandidates(limit = LIMIT)
            }
        val candidate = candidates.first()

        assertEquals(1, candidates.size)
        assertEquals(QUOTE_ID, candidate.quoteId)
        assertNull(candidate.roleTagId)
        assertEquals(emptyMap(), candidate.tagIdsByType)
    }

    @Test
    fun `quoteId 목록으로 후보를 조회하고 요청 순서를 유지한다`() {
        val candidates =
            withMockedResult({ dsl -> orderedCandidateResult(dsl) }) { dsl ->
                RecommendationCandidateRepository(dsl).findCandidatesByQuoteIds(listOf(2L, 1L))
            }

        assertEquals(listOf(2L, 1L), candidates.map { candidate -> candidate.quoteId })
    }

    private fun captureSql(repositoryCall: (DSLContext) -> Unit): String = captureQuery(repositoryCall).sql

    private fun captureQuery(repositoryCall: (DSLContext) -> Unit): CapturedQuery {
        var capturedSql = ""
        var capturedBindings = emptyList<Any?>()
        lateinit var dsl: DSLContext
        val connection =
            MockConnection(
                MockDataProvider { context ->
                    capturedSql = context.sql()
                    capturedBindings = context.bindings().toList()
                    arrayOf(MockResult(0, dsl.newResult(*CANDIDATE_FIELDS)))
                },
            )
        dsl = DSL.using(connection, SQLDialect.POSTGRES)
        repositoryCall(dsl)
        return CapturedQuery(sql = capturedSql, bindings = capturedBindings)
    }

    private fun findCandidatesWithResult(result: (DSLContext) -> Result<Record>) =
        withMockedResult(result) { dsl ->
            RecommendationCandidateRepository(dsl).findCandidates(
                effectiveTags = listOf(EFFECTIVE_EMOTION_TAG, EFFECTIVE_NEED_TAG),
                limit = LIMIT,
            )
        }

    private fun <T> withMockedResult(
        result: (DSLContext) -> Result<Record>,
        repositoryCall: (DSLContext) -> T,
    ): T {
        lateinit var dsl: DSLContext
        val connection =
            MockConnection(
                MockDataProvider {
                    arrayOf(MockResult(CANDIDATE_ROW_COUNT, result(dsl)))
                },
            )
        dsl = DSL.using(connection, SQLDialect.POSTGRES)
        return repositoryCall(dsl)
    }

    private fun candidateResult(dsl: DSLContext): Result<Record> =
        dsl.newResult(*CANDIDATE_FIELDS).apply {
            add(candidateRecord(dsl, ROLE_TAG_ID, TagType.ROLE))
            add(candidateRecord(dsl, EMOTION_TAG_ID, TagType.EMOTION))
            add(candidateRecord(dsl, NEED_TAG_ID, TagType.NEED))
            add(candidateRecord(dsl, MOOD_TAG_ID, TagType.MOOD))
        }

    private fun noTagCandidateResult(dsl: DSLContext): Result<Record> =
        dsl.newResult(*CANDIDATE_FIELDS).apply {
            add(
                dsl.newRecord(*CANDIDATE_FIELDS).apply {
                    set(QuoteTable.ID, QUOTE_ID)
                    set(QuoteTable.BOOK_ID, BOOK_ID)
                    set(QuoteTable.CONTENT, QUOTE_CONTENT)
                    set(BookTable.TITLE, BOOK_TITLE)
                    set(BookTable.AUTHOR, BOOK_AUTHOR)
                },
            )
        }

    private fun orderedCandidateResult(dsl: DSLContext): Result<Record> =
        dsl.newResult(*CANDIDATE_FIELDS).apply {
            add(candidateRecord(dsl, quoteId = 1L, tagId = EMOTION_TAG_ID, tagType = TagType.EMOTION))
            add(candidateRecord(dsl, quoteId = 2L, tagId = NEED_TAG_ID, tagType = TagType.NEED))
        }

    private fun candidateRecord(
        dsl: DSLContext,
        tagId: Long,
        tagType: TagType,
        quoteId: Long = QUOTE_ID,
    ): Record =
        dsl.newRecord(*CANDIDATE_FIELDS).apply {
            set(QuoteTable.ID, quoteId)
            set(QuoteTable.BOOK_ID, BOOK_ID)
            set(QuoteTable.CONTENT, QUOTE_CONTENT)
            set(BookTable.TITLE, BOOK_TITLE)
            set(BookTable.AUTHOR, BOOK_AUTHOR)
            set(TagTable.ID, tagId)
            set(TagTable.TYPE, tagType.name)
        }

    private companion object {
        const val LIMIT = 300
        const val QUOTE_ID = 10L
        const val BOOK_ID = 20L
        const val ROLE_TAG_ID = 30L
        const val EMOTION_TAG_ID = 31L
        const val NEED_TAG_ID = 32L
        const val MOOD_TAG_ID = 33L
        const val CONTEXT_TAG_ID = 34L
        const val CANDIDATE_ROW_COUNT = 4
        const val QUOTE_CONTENT = "좋은 문장"
        const val BOOK_TITLE = "좋은 책"
        const val BOOK_AUTHOR = "좋은 작가"
        val EFFECTIVE_EMOTION_TAG =
            EffectiveTag(
                tagId = EMOTION_TAG_ID,
                code = "EMOTION_STUCK",
                type = TagType.EMOTION,
            )
        val EFFECTIVE_NEED_TAG =
            EffectiveTag(
                tagId = NEED_TAG_ID,
                code = "NEED_COURAGE",
                type = TagType.NEED,
            )
        val EFFECTIVE_CONTEXT_TAG =
            EffectiveTag(
                tagId = CONTEXT_TAG_ID,
                code = "CONTEXT_HOME",
                type = TagType.CONTEXT,
            )
        val CANDIDATE_FIELDS: Array<Field<*>> =
            arrayOf(
                QuoteTable.ID,
                QuoteTable.BOOK_ID,
                QuoteTable.CONTENT,
                BookTable.TITLE,
                BookTable.AUTHOR,
                TagTable.ID,
                TagTable.TYPE,
            )
    }

    private data class CapturedQuery(
        val sql: String,
        val bindings: List<Any?>,
    )
}
