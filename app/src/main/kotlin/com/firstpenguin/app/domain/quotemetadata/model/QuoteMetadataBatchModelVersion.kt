package com.firstpenguin.app.domain.quotemetadata.model

enum class QuoteMetadataBatchModelVersion(
    val model: String,
    val version: Int,
) {
    V1(
        model = "gpt-5-nano",
        version = 1,
    ),
}
