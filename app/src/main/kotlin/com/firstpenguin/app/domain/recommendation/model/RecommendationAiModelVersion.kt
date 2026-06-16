package com.firstpenguin.app.domain.recommendation.model

enum class RecommendationAiModelVersion(
    val model: String,
    val version: Int,
) {
    USER_INPUT_ANALYSIS_V1(
        model = "gpt-5-mini",
        version = 1,
    ),
    ;

    companion object {
        private val modelVersionByModel = entries.associateBy { version -> version.model }

        fun fromModel(model: String): RecommendationAiModelVersion? = modelVersionByModel[model]
    }
}
