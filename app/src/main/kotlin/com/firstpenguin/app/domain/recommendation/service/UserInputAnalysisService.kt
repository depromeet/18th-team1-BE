package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.UserInputAnalysis
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

interface UserInputAnalysisService {
    fun analyze(input: RecommendationInput): UserInputAnalysis? = start(input).await()

    fun start(input: RecommendationInput): UserInputAnalysisTask
}

class UserInputAnalysisTask(
    private val canonicalAnalysis: CompletableFuture<UserInputAnalysis?>,
    private val analysis: CompletableFuture<UserInputAnalysis?>,
) {
    fun canonicalAnalysis(): CompletableFuture<UserInputAnalysis?> = canonicalAnalysis

    fun await(): UserInputAnalysis? = analysis.awaitResult()

    companion object {
        fun completed(analysis: UserInputAnalysis?): UserInputAnalysisTask {
            val completed = CompletableFuture.completedFuture(analysis)

            return UserInputAnalysisTask(completed, completed)
        }
    }
}

internal fun <T> CompletableFuture<T>.awaitResult(): T =
    try {
        get()
    } catch (exception: InterruptedException) {
        Thread.currentThread().interrupt()
        throw exception
    } catch (exception: ExecutionException) {
        throw exception.cause ?: exception
    }
