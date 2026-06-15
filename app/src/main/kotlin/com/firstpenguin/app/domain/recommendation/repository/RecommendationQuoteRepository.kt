package com.firstpenguin.app.domain.recommendation.repository

import com.firstpenguin.app.domain.recommendation.model.RankedRecommendationQuote
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidateSource
import com.firstpenguin.app.domain.recommendation.model.RecommendationQuote
import com.firstpenguin.app.domain.recommendation.model.RecommendationScoreBreakdown
import com.firstpenguin.app.domain.recommendation.repository.table.RecommendationQuoteTable
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.jooq.impl.DSL.max
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class RecommendationQuoteRepository(
    private val dsl: DSLContext,
) {
    fun insertRankedRecommendationQuotes(
        recommendationId: Long,
        rankedQuotes: List<RankedRecommendationQuote>,
        nextDisplayOrder: Int,
    ) {
        if (rankedQuotes.isEmpty()) return

        val now = LocalDateTime.now()
        var insertStep =
            dsl.insertInto(
                RecommendationQuoteTable.RECOMMENDATION_QUOTES,
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

        rankedQuotes.forEachIndexed { index, rankedQuote ->
            insertStep =
                insertStep.values(
                    recommendationId,
                    rankedQuote.quoteId,
                    nextDisplayOrder + index,
                    rankedQuote.source.name,
                    rankedQuote.score.needScore,
                    rankedQuote.score.emotionScore,
                    rankedQuote.score.contextScore,
                    rankedQuote.score.situationScore,
                    rankedQuote.score.moodScore,
                    rankedQuote.score.metadataScore,
                    rankedQuote.score.semanticScore,
                    rankedQuote.score.finalScore,
                    now,
                )
        }

        insertStep.execute()
    }

    fun getMaxDisplayOrder(recommendationId: Long): Int =
        dsl
            .select(DSL.coalesce(max(RecommendationQuoteTable.DISPLAY_ORDER), 0))
            .from(RecommendationQuoteTable.RECOMMENDATION_QUOTES)
            .where(RecommendationQuoteTable.RECOMMENDATION_ID.eq(recommendationId))
            .fetchOne(0, Int::class.java)!!

    fun findByRecommendationId(recommendationId: Long): List<RecommendationQuote> =
        dsl
            .select(RECOMMENDATION_QUOTE_FIELDS)
            .from(RecommendationQuoteTable.RECOMMENDATION_QUOTES)
            .where(RecommendationQuoteTable.RECOMMENDATION_ID.eq(recommendationId))
            .orderBy(RecommendationQuoteTable.DISPLAY_ORDER.asc())
            .fetch(::toRecommendationQuote)

    fun existsByRecommendationIdAndQuoteId(
        recommendationId: Long,
        quoteId: Long,
    ): Boolean =
        dsl.fetchExists(
            RecommendationQuoteTable.RECOMMENDATION_QUOTES,
            RecommendationQuoteTable.RECOMMENDATION_ID
                .eq(recommendationId)
                .and(RecommendationQuoteTable.QUOTE_ID.eq(quoteId)),
        )

    fun deleteByRecommendationId(recommendationId: Long) {
        dsl
            .deleteFrom(RecommendationQuoteTable.RECOMMENDATION_QUOTES)
            .where(RecommendationQuoteTable.RECOMMENDATION_ID.eq(recommendationId))
            .execute()
    }

    private fun toRecommendationQuote(record: Record): RecommendationQuote =
        RecommendationQuote(
            id = record[RecommendationQuoteTable.ID]!!,
            recommendationId = record[RecommendationQuoteTable.RECOMMENDATION_ID]!!,
            quoteId = record[RecommendationQuoteTable.QUOTE_ID]!!,
            displayOrder = record[RecommendationQuoteTable.DISPLAY_ORDER]!!,
            candidateSource = record.toCandidateSource(),
            score = record.toScoreBreakdown(),
            createdAt = record[RecommendationQuoteTable.CREATED_AT]!!,
        )

    private fun Record.toCandidateSource(): RecommendationCandidateSource? =
        this[RecommendationQuoteTable.CANDIDATE_SOURCE]
            ?.let(RecommendationCandidateSource::valueOf)

    private fun Record.toScoreBreakdown(): RecommendationScoreBreakdown? {
        val finalScore = this[RecommendationQuoteTable.FINAL_SCORE] ?: return null

        return RecommendationScoreBreakdown(
            needScore = this[RecommendationQuoteTable.NEED_SCORE] ?: DEFAULT_SCORE,
            emotionScore = this[RecommendationQuoteTable.EMOTION_SCORE] ?: DEFAULT_SCORE,
            contextScore = this[RecommendationQuoteTable.CONTEXT_SCORE] ?: DEFAULT_SCORE,
            situationScore = this[RecommendationQuoteTable.SITUATION_SCORE] ?: DEFAULT_SCORE,
            moodScore = this[RecommendationQuoteTable.MOOD_SCORE] ?: DEFAULT_SCORE,
            metadataScore = this[RecommendationQuoteTable.METADATA_SCORE] ?: DEFAULT_SCORE,
            semanticScore = this[RecommendationQuoteTable.SEMANTIC_SCORE] ?: DEFAULT_SCORE,
            finalScore = finalScore,
        )
    }

    private companion object {
        const val DEFAULT_SCORE = 0.0

        val RECOMMENDATION_QUOTE_FIELDS: List<Field<*>> =
            listOf(
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
