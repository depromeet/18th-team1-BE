package com.firstpenguin.app.domain.recommendation.model

data class UserSemanticEmbedding(
    val inputText: String,
    val embedding: List<Double>,
)
