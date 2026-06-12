package com.firstpenguin.app.domain.quotebatch.model

enum class QuoteBatchModelVersion(
    val model: String,
    val version: Int,
) {
    QUOTE_METADATA_V1(
        model = "gpt-5-mini",
        version = 1,
    ),
    QUOTE_EXTRACTION_V1(
        model = "gpt-5-mini",
        version = 1,
    ),
    QUOTE_REVIEW_V1(
        model = "gpt-5-mini",
        version = 1,
    ),
}
