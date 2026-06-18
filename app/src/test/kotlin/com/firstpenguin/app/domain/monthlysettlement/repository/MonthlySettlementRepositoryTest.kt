package com.firstpenguin.app.domain.monthlysettlement.repository

import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementCreateCommand
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementEmotionTag
import com.firstpenguin.app.domain.monthlysettlement.repository.table.MonthlySettlementTable
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MonthlySettlementRepositoryTest {
    @Test
    fun `함께한 문장 수는 recommendation_quotes 기준으로 집계한다`() {
        val capturedSql =
            captureSql { dsl ->
                MonthlySettlementQuoteAggregationRepository(dsl).countRecommendedQuotes(
                    userId = USER_ID,
                    start = START,
                    endExclusive = END_EXCLUSIVE,
                )
            }
        val normalizedSql = capturedSql.normalized()

        assertTrue(normalizedSql.contains("recommendation_quotes"), normalizedSql)
        assertTrue(
            normalizedSql.contains("\"recommendations\".\"recommendation_date\" >= cast(? as date)"),
            normalizedSql,
        )
        assertTrue(
            normalizedSql.contains("\"recommendations\".\"recommendation_date\" < cast(? as date)"),
            normalizedSql,
        )
        assertFalse(normalizedSql.contains("daily_recommendation_quotes"), normalizedSql)
    }

    @Test
    fun `가장 많이 만난 장르는 genre 기준 테이블 라벨을 문장 기준으로 집계한다`() {
        val capturedSql =
            captureSql { dsl ->
                MonthlySettlementQuoteAggregationRepository(dsl).findMostFrequentGenre(
                    userId = USER_ID,
                    start = START,
                    endExclusive = END_EXCLUSIVE,
                )
            }
        val normalizedSql = capturedSql.normalized()

        assertTrue(normalizedSql.contains("join \"genres\""), normalizedSql)
        assertTrue(normalizedSql.contains("\"genres\".\"label\""), normalizedSql)
        assertTrue(normalizedSql.contains("\"genres\".\"label\" is not null"), normalizedSql)
        assertTrue(normalizedSql.contains("trim(\"genres\".\"label\") <> ?"), normalizedSql)
        assertTrue(
            normalizedSql.contains("order by count(\"recommendation_quotes\".\"id\") desc, \"genres\".\"label\" asc"),
            normalizedSql,
        )
    }

    @Test
    fun `감정 태그는 recommendation_tags 기준으로 집계하고 추천 문장 수로 곱하지 않는다`() {
        val capturedSql =
            captureSql { dsl ->
                MonthlySettlementEmotionAggregationRepository(dsl).findEmotionTagCounts(
                    userId = USER_ID,
                    start = START,
                    endExclusive = END_EXCLUSIVE,
                    limit = EMOTION_TAG_LIMIT,
                )
            }
        val normalizedSql = capturedSql.normalized()

        assertTrue(normalizedSql.contains("recommendation_tags"), normalizedSql)
        assertTrue(normalizedSql.contains("\"tags\".\"type\" = ?"), normalizedSql)
        assertTrue(
            normalizedSql.contains(
                "order by \"tag_count\" desc, \"tags\".\"sort_order\" asc, \"tags\".\"id\" asc",
            ),
            normalizedSql,
        )
        assertFalse(normalizedSql.contains("recommendation_quotes"), normalizedSql)
    }

    @Test
    fun `이달의 책 후보는 사용자 월 추천 이력 안에서 quote metadata tag 기준으로 랜덤 선택한다`() {
        val capturedSql =
            captureSql { dsl ->
                MonthlySettlementEmotionAggregationRepository(dsl).findMonthlyBookCandidateByEmotionTagId(
                    userId = USER_ID,
                    start = START,
                    endExclusive = END_EXCLUSIVE,
                    tagId = TAG_ID,
                )
            }
        val normalizedSql = capturedSql.normalized()

        assertTrue(normalizedSql.contains("recommendation_quotes"), normalizedSql)
        assertTrue(normalizedSql.contains("join \"genres\""), normalizedSql)
        assertTrue(normalizedSql.contains("\"books\".\"aladin_link\""), normalizedSql)
        assertTrue(normalizedSql.contains("\"recommendations\".\"user_id\" = ?"), normalizedSql)
        assertTrue(
            normalizedSql.contains("\"recommendations\".\"recommendation_date\" >= cast(? as date)"),
            normalizedSql,
        )
        assertTrue(
            normalizedSql.contains("\"recommendations\".\"recommendation_date\" < cast(? as date)"),
            normalizedSql,
        )
        assertTrue(normalizedSql.contains("quote_metadata_tags"), normalizedSql)
        assertTrue(normalizedSql.contains("quote_metadata"), normalizedSql)
        assertTrue(normalizedSql.contains("\"quote_metadata_tags\".\"tag_id\" = ?"), normalizedSql)
        assertTrue(normalizedSql.contains("order by random()"), normalizedSql)
    }

    @Test
    fun `월말 결산 저장은 월 단위 unique conflict를 무시한다`() {
        val capturedSql =
            captureSql { dsl ->
                MonthlySettlementCommandRepository(dsl).insertMonthlySettlement(settlementCommand())
            }
        val normalizedSql = capturedSql.normalized()

        assertTrue(normalizedSql.contains("insert into \"monthly_settlements\""), normalizedSql)
        assertTrue(
            normalizedSql.contains(
                "on conflict (\"user_id\", \"settlement_year\", \"settlement_month\") do nothing",
            ),
            normalizedSql,
        )
    }

    private fun captureSql(repositoryCall: (DSLContext) -> Unit): String {
        var capturedSql = ""
        lateinit var dsl: DSLContext
        val connection =
            MockConnection(
                MockDataProvider { context ->
                    capturedSql = context.sql()
                    arrayOf(MockResult(0, dsl.newResult(MonthlySettlementTable.ID)))
                },
            )
        dsl = DSL.using(connection, SQLDialect.POSTGRES)
        repositoryCall(dsl)
        return capturedSql
    }

    private fun String.normalized(): String = replace(Regex("\\s+"), " ")

    private fun settlementCommand(): MonthlySettlementCreateCommand =
        MonthlySettlementCreateCommand(
            userId = USER_ID,
            year = YEAR,
            month = MONTH,
            sharedQuoteCount = SHARED_QUOTE_COUNT,
            mostFrequentGenre = GENRE,
            topEmotionTag =
                MonthlySettlementEmotionTag(
                    tagId = TAG_ID,
                    label = TAG_LABEL,
                    count = TAG_COUNT,
                    sortOrder = 1,
                ),
            recommendationMessage = "$TAG_LABEL ${MONTH}월을 보내셨군요. 이 감정과 유사한 문장이 담긴 책을 추천해요.",
            monthlyBook = null,
            monthlyBooks = emptyList(),
            emotionTags =
                listOf(
                    MonthlySettlementEmotionTag(
                        tagId = TAG_ID,
                        label = TAG_LABEL,
                        count = TAG_COUNT,
                        sortOrder = 1,
                    ),
                ),
        )

    private companion object {
        const val USER_ID = 1L
        const val YEAR = 2026
        const val MONTH = 3
        const val SHARED_QUOTE_COUNT = 27
        const val TAG_ID = 10L
        const val TAG_LABEL = "무기력한"
        const val TAG_COUNT = 5
        const val EMOTION_TAG_LIMIT = 10
        const val GENRE = "추리/미스터리 소설"
        val START: LocalDate = LocalDate.of(YEAR, MONTH, 1)
        val END_EXCLUSIVE: LocalDate = YearMonth.of(YEAR, MONTH).plusMonths(1).atDay(1)
    }
}
