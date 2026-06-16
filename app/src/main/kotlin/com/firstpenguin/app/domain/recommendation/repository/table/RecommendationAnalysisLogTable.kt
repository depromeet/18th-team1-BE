package com.firstpenguin.app.domain.recommendation.repository.table

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object RecommendationAnalysisLogTable {
    val RECOMMENDATION_ANALYSIS_LOGS = DSL.table(DSL.name("recommendation_analysis_logs"))
    val ID = DSL.field(DSL.name("recommendation_analysis_logs", "id"), Long::class.java)
    val RECOMMENDATION_ID =
        DSL.field(DSL.name("recommendation_analysis_logs", "recommendation_id"), Long::class.java)
    val LLM_MODEL = DSL.field(DSL.name("recommendation_analysis_logs", "llm_model"), String::class.java)
    val LLM_MODEL_VERSION =
        DSL.field(DSL.name("recommendation_analysis_logs", "llm_model_version"), Int::class.java)
    val CANONICAL_INTENT =
        DSL.field(DSL.name("recommendation_analysis_logs", "canonical_intent"), String::class.java)
    val EMBEDDING_INPUT_TEXT =
        DSL.field(DSL.name("recommendation_analysis_logs", "embedding_input_text"), String::class.java)
    val PROMPT_CACHE_HIT =
        DSL.field(DSL.name("recommendation_analysis_logs", "prompt_cache_hit"), Boolean::class.java)
    val INPUT_TOKENS = DSL.field(DSL.name("recommendation_analysis_logs", "input_tokens"), Long::class.java)
    val CACHED_TOKENS = DSL.field(DSL.name("recommendation_analysis_logs", "cached_tokens"), Long::class.java)
    val OUTPUT_TOKENS = DSL.field(DSL.name("recommendation_analysis_logs", "output_tokens"), Long::class.java)
    val LLM_ELAPSED_MS = DSL.field(DSL.name("recommendation_analysis_logs", "llm_elapsed_ms"), Long::class.java)
    val EMBEDDING_ELAPSED_MS =
        DSL.field(DSL.name("recommendation_analysis_logs", "embedding_elapsed_ms"), Long::class.java)
    val CREATED_AT = DSL.field(DSL.name("recommendation_analysis_logs", "created_at"), LocalDateTime::class.java)
}
