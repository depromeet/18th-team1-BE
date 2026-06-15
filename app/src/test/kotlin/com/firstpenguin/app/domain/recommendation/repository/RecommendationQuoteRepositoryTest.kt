package com.firstpenguin.app.domain.recommendation.repository

import com.firstpenguin.app.domain.recommendation.model.RankedRecommendationQuote
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidateSource
import com.firstpenguin.app.domain.recommendation.model.RecommendationScoreBreakdown
import com.firstpenguin.app.domain.recommendation.repository.table.RecommendationQuoteTable
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class RecommendationQuoteRepositoryTest {
    @Test
    fun `ranked recommendation quote 저장 시 score 필드를 함께 저장한다`() {
        val capturedSql =
            captureSql { dsl ->
                RecommendationQuoteRepository(dsl).insertRankedRecommendationQuotes(
                    recommendationId = RECOMMENDATION_ID,
                    rankedQuotes = listOf(rankedQuote()),
                    nextDisplayOrder = DISPLAY_ORDER,
                )
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.contains("\"need_score\""), normalizedSql)
        assertTrue(normalizedSql.contains("\"candidate_source\""), normalizedSql)
        assertTrue(normalizedSql.contains("\"emotion_score\""), normalizedSql)
        assertTrue(normalizedSql.contains("\"metadata_score\""), normalizedSql)
        assertTrue(normalizedSql.contains("\"final_score\""), normalizedSql)
    }

    @Test
    fun `recommendation quote 조회 시 score breakdown을 복원한다`() {
        val recommendationQuotes =
            withMockedResult({ dsl -> recommendationQuoteResult(dsl) }) { dsl ->
                RecommendationQuoteRepository(dsl).findByRecommendationId(RECOMMENDATION_ID)
            }
        val recommendationQuote = recommendationQuotes.first()

        assertEquals(QUOTE_ID, recommendationQuote.quoteId)
        assertEquals(RecommendationCandidateSource.PRIMARY, recommendationQuote.candidateSource)
        assertNotNull(recommendationQuote.score)
        assertEquals(NEED_SCORE, recommendationQuote.score?.needScore)
        assertEquals(FINAL_SCORE, recommendationQuote.score?.finalScore)
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

    private fun <T> withMockedResult(
        result: (DSLContext) -> Result<Record>,
        repositoryCall: (DSLContext) -> T,
    ): T {
        lateinit var dsl: DSLContext
        val connection =
            MockConnection(
                MockDataProvider {
                    arrayOf(MockResult(1, result(dsl)))
                },
            )
        dsl = DSL.using(connection, SQLDialect.POSTGRES)
        return repositoryCall(dsl)
    }

    private fun recommendationQuoteResult(dsl: DSLContext): Result<Record> =
        dsl.newResult(*RECOMMENDATION_QUOTE_FIELDS).apply {
            add(
                dsl.newRecord(*RECOMMENDATION_QUOTE_FIELDS).apply {
                    set(RecommendationQuoteTable.ID, RECOMMENDATION_QUOTE_ID)
                    set(RecommendationQuoteTable.RECOMMENDATION_ID, RECOMMENDATION_ID)
                    set(RecommendationQuoteTable.QUOTE_ID, QUOTE_ID)
                    set(RecommendationQuoteTable.DISPLAY_ORDER, DISPLAY_ORDER)
                    set(RecommendationQuoteTable.CANDIDATE_SOURCE, RecommendationCandidateSource.PRIMARY.name)
                    set(RecommendationQuoteTable.NEED_SCORE, NEED_SCORE)
                    set(RecommendationQuoteTable.EMOTION_SCORE, EMOTION_SCORE)
                    set(RecommendationQuoteTable.CONTEXT_SCORE, CONTEXT_SCORE)
                    set(RecommendationQuoteTable.SITUATION_SCORE, SITUATION_SCORE)
                    set(RecommendationQuoteTable.MOOD_SCORE, MOOD_SCORE)
                    set(RecommendationQuoteTable.METADATA_SCORE, METADATA_SCORE)
                    set(RecommendationQuoteTable.SEMANTIC_SCORE, SEMANTIC_SCORE)
                    set(RecommendationQuoteTable.FINAL_SCORE, FINAL_SCORE)
                    set(RecommendationQuoteTable.CREATED_AT, CREATED_AT)
                },
            )
        }

    private fun rankedQuote(): RankedRecommendationQuote =
        RankedRecommendationQuote(
            rank = DISPLAY_ORDER,
            candidate =
                RecommendationCandidate(
                    quoteId = QUOTE_ID,
                    bookId = BOOK_ID,
                    content = "content",
                    title = "title",
                    author = "author",
                    roleTagId = null,
                    tagIdsByType = emptyMap(),
                ),
            score = scoreBreakdown(),
        )

    private fun scoreBreakdown(): RecommendationScoreBreakdown =
        RecommendationScoreBreakdown(
            needScore = NEED_SCORE,
            emotionScore = EMOTION_SCORE,
            contextScore = CONTEXT_SCORE,
            situationScore = SITUATION_SCORE,
            moodScore = MOOD_SCORE,
            metadataScore = METADATA_SCORE,
            semanticScore = SEMANTIC_SCORE,
            finalScore = FINAL_SCORE,
        )

    private companion object {
        const val RECOMMENDATION_QUOTE_ID = 1L
        const val RECOMMENDATION_ID = 2L
        const val QUOTE_ID = 3L
        const val BOOK_ID = 4L
        const val DISPLAY_ORDER = 1
        const val NEED_SCORE = 0.1
        const val EMOTION_SCORE = 0.2
        const val CONTEXT_SCORE = 0.3
        const val SITUATION_SCORE = 0.4
        const val MOOD_SCORE = 0.5
        const val METADATA_SCORE = 0.6
        const val SEMANTIC_SCORE = 0.7
        const val FINAL_SCORE = 0.8

        val CREATED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 15, 0, 0)
        val RECOMMENDATION_QUOTE_FIELDS =
            arrayOf(
                RecommendationQuoteTable.ID,
                RecommendationQuoteTable.RECOMMENDATION_ID,
                RecommendationQuoteTable.QUOTE_ID,
                RecommendationQuoteTable.DISPLAY_ORDER,
                RecommendationQuoteTable.CANDIDATE_SOURCE,
                RecommendationQuoteTable.NEED_SCORE,
                RecommendationQuoteTable.EMOTION_SCORE,
                RecommendationQuoteTable.CONTEXT_SCORE,
                RecommendationQuoteTable.SITUATION_SCORE,
                RecommendationQuoteTable.MOOD_SCORE,
                RecommendationQuoteTable.METADATA_SCORE,
                RecommendationQuoteTable.SEMANTIC_SCORE,
                RecommendationQuoteTable.FINAL_SCORE,
                RecommendationQuoteTable.CREATED_AT,
            )
    }
}
