package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.RecommendationAnalysisLog
import com.firstpenguin.app.domain.recommendation.repository.RecommendationAnalysisLogRepository
import org.springframework.stereotype.Service

@Service
class RecommendationAnalysisLogService(
    private val recommendationAnalysisLogRepository: RecommendationAnalysisLogRepository,
) {
    fun saveRecommendationAnalysisLog(
        recommendationId: Long,
        analysisLog: RecommendationAnalysisLog,
    ) {
        recommendationAnalysisLogRepository.upsertRecommendationAnalysisLog(
            recommendationId = recommendationId,
            analysisLog = analysisLog,
        )
    }
}
