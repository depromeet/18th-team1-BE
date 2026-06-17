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
            candidateFuture = async { candidateProvider.findCandidates(selectedEffectiveTags) },
            moodTagIdByCodeFuture = async { tagRepository.getActiveMoodTagIdByCode() },
            tagRarityWeightsFuture = async { tagRarityRepository.findMetadataTagRarityWeights() },
        )

    fun refreshCandidates(
        prefetch: RecommendationEnginePrefetch,
        effectiveTags: Collection<EffectiveTag>,
    ): RecommendationEnginePrefetch {
        val refreshedCandidates = async { candidateProvider.findCandidates(effectiveTags) }

        return prefetch.replaceCandidates(refreshedCandidates)
    }

    private fun <T> async(block: () -> T): CompletableFuture<T> = CompletableFuture.supplyAsync(block, executor)
}

class RecommendationEnginePrefetch(
    private val candidateFuture: CompletableFuture<List<RecommendationCandidate>>,
    private val moodTagIdByCodeFuture: CompletableFuture<Map<String, Long>>,
    private val tagRarityWeightsFuture: CompletableFuture<Map<Long, Double>>,
) {
    fun candidates(): List<RecommendationCandidate> = candidateFuture.await()

    fun moodTagIdByCode(): Map<String, Long> = moodTagIdByCodeFuture.await()

    fun tagRarityWeights(): Map<Long, Double> = tagRarityWeightsFuture.await()

    fun replaceCandidates(candidates: CompletableFuture<List<RecommendationCandidate>>): RecommendationEnginePrefetch =
        RecommendationEnginePrefetch(
            candidateFuture = candidates,
            moodTagIdByCodeFuture = moodTagIdByCodeFuture,
            tagRarityWeightsFuture = tagRarityWeightsFuture,
        )
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
