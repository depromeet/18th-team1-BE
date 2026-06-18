package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.embedding.repository.QuoteEmbeddingRepository
import com.firstpenguin.app.domain.embedding.usecase.UserQueryEmbeddingProcessor
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.UserSemanticEmbedding
import com.firstpenguin.app.domain.recommendation.repository.RecommendationCandidateProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

private typealias OptionalEmbedding = MeasuredRecommendationValue<List<Double>?>

interface RecommendationSemanticProvider {
    fun prepare(input: RecommendationInput): UserSemanticEmbedding?

    fun findScores(
        userEmbedding: UserSemanticEmbedding?,
        quoteIds: Collection<Long>,
    ): Map<Long, Double>

    fun findSimilarCandidates(
        userEmbedding: UserSemanticEmbedding?,
        excludedQuoteIds: Collection<Long>,
        limit: Int = DEFAULT_SIMILAR_CANDIDATE_LIMIT,
    ): List<RecommendationCandidate>
}

@Component
class RecommendationSemanticService(
    private val userEmbeddingInputBuilder: UserEmbeddingInputBuilder,
    private val userQueryEmbeddingProcessor: UserQueryEmbeddingProcessor,
    private val quoteEmbeddingRepository: QuoteEmbeddingRepository,
    private val candidateProvider: RecommendationCandidateProvider,
    @Qualifier(RECOMMENDATION_EMBEDDING_EXECUTOR_NAME) private val embeddingExecutor: ExecutorService,
) : RecommendationSemanticProvider {
    override fun prepare(input: RecommendationInput): UserSemanticEmbedding? =
        userEmbeddingInputBuilder
            .build(input)
            ?.toUserSemanticEmbedding()

    private fun String.toUserSemanticEmbedding(): UserSemanticEmbedding? {
        val embedding =
            runCatching {
                measureRecommendationElapsed {
                    embedWithTimeout(this)
                }
            }.getOrNull() ?: return null

        return embedding.toUserSemanticEmbedding(this)
    }

    private fun OptionalEmbedding.toUserSemanticEmbedding(inputText: String): UserSemanticEmbedding? {
        val embeddingValue = value ?: return null
        return UserSemanticEmbedding(
            inputText = inputText,
            embedding = embeddingValue,
            embeddingElapsedMs = elapsedMs,
        )
    }

    private fun embedWithTimeout(input: String): List<Double>? {
        val future =
            embeddingExecutor.submit<List<Double>> {
                userQueryEmbeddingProcessor.embed(input)
            }

        return runCatching {
            future.get(EMBEDDING_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        }.onFailure { exception ->
            future.cancel(true)
            if (exception is InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }.getOrNull()
    }

    override fun findScores(
        userEmbedding: UserSemanticEmbedding?,
        quoteIds: Collection<Long>,
    ): Map<Long, Double> {
        if (userEmbedding == null) return emptyMap()

        return quoteEmbeddingRepository.findCosineSimilarities(
            quoteIds = quoteIds,
            userEmbedding = userEmbedding.embedding,
        )
    }

    override fun findSimilarCandidates(
        userEmbedding: UserSemanticEmbedding?,
        excludedQuoteIds: Collection<Long>,
        limit: Int,
    ): List<RecommendationCandidate> {
        if (userEmbedding == null) return emptyList()
        val quoteIds =
            quoteEmbeddingRepository.findMostSimilarQuoteIds(
                userEmbedding = userEmbedding.embedding,
                excludedQuoteIds = excludedQuoteIds,
                limit = limit,
            )

        return candidateProvider.findCandidatesByQuoteIds(quoteIds)
    }

    private companion object {
        const val EMBEDDING_TIMEOUT_MILLIS = 8_000L
    }
}

private const val DEFAULT_SIMILAR_CANDIDATE_LIMIT = 300
