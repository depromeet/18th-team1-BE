package com.firstpenguin.app.domain.openai.dto

data class OpenAiBatchOutputItem(
    val customId: String,
    val outputText: String?,
    val errorMessage: String?,
)
