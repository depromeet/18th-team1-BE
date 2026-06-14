package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import org.springframework.stereotype.Component

@Component
class UserEmbeddingInputBuilder {
    fun build(input: RecommendationInput): String? =
        input.analysis
            ?.canonicalIntent
            ?.trim()
            ?.takeIf { canonicalIntent -> canonicalIntent.isNotEmpty() }
}
