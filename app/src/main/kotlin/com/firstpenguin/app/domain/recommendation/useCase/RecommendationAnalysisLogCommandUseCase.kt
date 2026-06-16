package com.firstpenguin.app.domain.recommendation.useCase

import com.firstpenguin.app.domain.recommendation.model.RecommendationAnalysisLog
import com.firstpenguin.app.domain.recommendation.service.RecommendationAnalysisLogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class RecommendationAnalysisLogCommandUseCase(
    private val recommendationAnalysisLogService: RecommendationAnalysisLogService,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveRecommendationAnalysisLog(
        recommendationId: Long,
        analysisLog: RecommendationAnalysisLog,
    ) {
        recommendationAnalysisLogService.saveRecommendationAnalysisLog(
            recommendationId = recommendationId,
            analysisLog = analysisLog,
        )
    }
}
