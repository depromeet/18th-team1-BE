package com.firstpenguin.app.global.enums

enum class QuoteMetadataBatchModelVersion(
    val model: String,
    val version: Int,
) {
    V1(
        model = "gpt-5-nano",
        version = 1,
    ),
}
