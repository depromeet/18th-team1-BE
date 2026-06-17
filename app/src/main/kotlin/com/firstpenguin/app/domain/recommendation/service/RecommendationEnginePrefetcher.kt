package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.emotion.repository.TagRepository
import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.repository.RecommendationCandidateProvider
import com.firstpenguin.app.domain.recommendation.repository.RecommendationTagRarityRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

@Component
class RecommendationEnginePrefetcher(
    private val candidateProvider: RecommendationCandidateProvider,
    private val tagRepository: TagRepository,
    private val tagRarityRepository: RecommendationTagRarityRepository,
    @Qualifier(RECOMMENDATION_PREFETCH_EXECUTOR_NAME) private val executor: Executor,
) {
    fun start(selectedEffectiveTags: Collection<EffectiveTag>): RecommendationEnginePrefetch =
        RecommendationEnginePrefetch(
            candidates = async { candidateProvider.findCandidates(selectedEffectiveTags) },
            moodTagIdByCode = async { tagRepository.getActiveMoodTagIdByCode() },
            tagRarityWeights = async { tagRarityRepository.findMetadataTagRarityWeights() },
        )

    private fun <T> async(block: () -> T): CompletableFuture<T> = CompletableFuture.supplyAsync(block, executor)
}

class RecommendationEnginePrefetch(
    private val candidates: CompletableFuture<List<RecommendationCandidate>>,
    private val moodTagIdByCode: CompletableFuture<Map<String, Long>>,
    private val tagRarityWeights: CompletableFuture<Map<Long, Double>>,
) {
    fun candidates(): List<RecommendationCandidate> = candidates.await()

    fun moodTagIdByCode(): Map<String, Long> = moodTagIdByCode.await()

    fun tagRarityWeights(): Map<Long, Double> = tagRarityWeights.await()
}

private fun <T> CompletableFuture<T>.await(): T =
    try {
        get()
    } catch (exception: InterruptedException) {
        Thread.currentThread().interrupt()
        throw exception
    } catch (exception: ExecutionException) {
        throw exception.cause ?: exception
    }
