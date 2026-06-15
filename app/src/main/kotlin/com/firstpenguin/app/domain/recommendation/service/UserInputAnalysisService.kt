package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.UserInputAnalysis

interface UserInputAnalysisService {
    fun analyze(input: RecommendationInput): UserInputAnalysis?
}
