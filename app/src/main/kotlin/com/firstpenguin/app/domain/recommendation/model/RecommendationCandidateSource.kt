package com.firstpenguin.app.domain.recommendation.model

enum class RecommendationCandidateSource {
    PRIMARY,
    FALLBACK_NEED,
    FALLBACK_EMOTION,
    FALLBACK_SEMANTIC,
    FALLBACK_RELAXED,
    FALLBACK_RANDOM,
}
