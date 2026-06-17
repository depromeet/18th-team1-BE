package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import org.springframework.stereotype.Component

@Component
class UserEmbeddingInputBuilder {
    fun build(input: RecommendationInput): String? =
        canonicalIntent(input)
            ?: fallbackInputText(input)

    private fun canonicalIntent(input: RecommendationInput): String? =
        input.analysis
            ?.canonicalIntent
            ?.trim()
            ?.takeIf { canonicalIntent -> canonicalIntent.isNotEmpty() }

    private fun fallbackInputText(input: RecommendationInput): String? =
        listOfNotNull(
            input.feelingText.normalizedText()?.let { text -> "feelingText: $text" },
            input.diaryText.normalizedText()?.let { text -> "diaryText: $text" },
        ).joinToString("\n")
            .takeIf { text -> text.isNotBlank() }

    private fun String?.normalizedText(): String? = this?.trim()?.takeIf { text -> text.isNotEmpty() }
}
