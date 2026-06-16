package com.firstpenguin.app.domain.recommendation.repository

import com.firstpenguin.app.domain.recommendation.model.RecommendationAnalysisLog
import com.firstpenguin.app.domain.recommendation.repository.table.RecommendationAnalysisLogTable
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class RecommendationAnalysisLogRepository(
    private val dsl: DSLContext,
) {
    fun upsertRecommendationAnalysisLog(
        recommendationId: Long,
        analysisLog: RecommendationAnalysisLog,
    ) {
        val now = LocalDateTime.now()

        dsl
            .insertInto(RecommendationAnalysisLogTable.RECOMMENDATION_ANALYSIS_LOGS)
            .set(RecommendationAnalysisLogTable.RECOMMENDATION_ID, recommendationId)
            .set(RecommendationAnalysisLogTable.LLM_MODEL, analysisLog.llmModel)
            .set(RecommendationAnalysisLogTable.LLM_MODEL_VERSION, analysisLog.llmModelVersion)
            .set(RecommendationAnalysisLogTable.CANONICAL_INTENT, analysisLog.canonicalIntent)
            .set(RecommendationAnalysisLogTable.EMBEDDING_INPUT_TEXT, analysisLog.embeddingInputText)
            .set(RecommendationAnalysisLogTable.PROMPT_CACHE_HIT, analysisLog.hasPromptCacheHit())
            .set(RecommendationAnalysisLogTable.INPUT_TOKENS, analysisLog.inputTokens)
            .set(RecommendationAnalysisLogTable.CACHED_TOKENS, analysisLog.cachedTokens)
            .set(RecommendationAnalysisLogTable.OUTPUT_TOKENS, analysisLog.outputTokens)
            .set(RecommendationAnalysisLogTable.LLM_ELAPSED_MS, analysisLog.llmElapsedMs)
            .set(RecommendationAnalysisLogTable.EMBEDDING_ELAPSED_MS, analysisLog.embeddingElapsedMs)
            .set(RecommendationAnalysisLogTable.CREATED_AT, now)
            .onConflict(RecommendationAnalysisLogTable.RECOMMENDATION_ID)
            .doUpdate()
            .set(RecommendationAnalysisLogTable.LLM_MODEL, analysisLog.llmModel)
            .set(RecommendationAnalysisLogTable.LLM_MODEL_VERSION, analysisLog.llmModelVersion)
            .set(RecommendationAnalysisLogTable.CANONICAL_INTENT, analysisLog.canonicalIntent)
            .set(RecommendationAnalysisLogTable.EMBEDDING_INPUT_TEXT, analysisLog.embeddingInputText)
            .set(RecommendationAnalysisLogTable.PROMPT_CACHE_HIT, analysisLog.hasPromptCacheHit())
            .set(RecommendationAnalysisLogTable.INPUT_TOKENS, analysisLog.inputTokens)
            .set(RecommendationAnalysisLogTable.CACHED_TOKENS, analysisLog.cachedTokens)
            .set(RecommendationAnalysisLogTable.OUTPUT_TOKENS, analysisLog.outputTokens)
            .set(RecommendationAnalysisLogTable.LLM_ELAPSED_MS, analysisLog.llmElapsedMs)
            .set(RecommendationAnalysisLogTable.EMBEDDING_ELAPSED_MS, analysisLog.embeddingElapsedMs)
            .execute()
    }
}

private fun RecommendationAnalysisLog.hasPromptCacheHit(): Boolean = (cachedTokens ?: NO_CACHED_TOKEN) > NO_CACHED_TOKEN

private const val NO_CACHED_TOKEN = 0L
