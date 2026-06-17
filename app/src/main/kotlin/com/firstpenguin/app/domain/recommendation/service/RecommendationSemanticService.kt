package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.embedding.repository.QuoteEmbeddingRepository
import com.firstpenguin.app.domain.embedding.usecase.UserQueryEmbeddingProcessor
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.UserSemanticEmbedding
import com.firstpenguin.app.domain.recommendation.repository.RecommendationCandidateProvider
import org.springframework.stereotype.Component

interface RecommendationSemanticProvider {
    fun prepare(input: RecommendationInput): UserSemanticEmbedding?

    fun findScores(
        userEmbedding: UserSemanticEmbedding?,
        quoteIds: Collection<Long>,
    ): Map<Long, Double>

    fun findSimilarCandidates(
        userEmbedding: UserSemanticEmbedding?,
        excludedQuoteIds: Collection<Long>,
    ): List<RecommendationCandidate>
}

@Component
class RecommendationSemanticService(
    private val userEmbeddingInputBuilder: UserEmbeddingInputBuilder,
    private val userQueryEmbeddingProcessor: UserQueryEmbeddingProcessor,
    private val quoteEmbeddingRepository: QuoteEmbeddingRepository,
    private val candidateProvider: RecommendationCandidateProvider,
) : RecommendationSemanticProvider {
    override fun prepare(input: RecommendationInput): UserSemanticEmbedding? {
        val embeddingInput = userEmbeddingInputBuilder.build(input) ?: return null
        val embedding =
            measureRecommendationElapsed {
                userQueryEmbeddingProcessor.embed(embeddingInput)
            }

        return UserSemanticEmbedding(
            inputText = embeddingInput,
            embedding = embedding.value,
            embeddingElapsedMs = embedding.elapsedMs,
        )
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
    ): List<RecommendationCandidate> {
        if (userEmbedding == null) return emptyList()
        val quoteIds =
            quoteEmbeddingRepository.findMostSimilarQuoteIds(
                userEmbedding = userEmbedding.embedding,
                excludedQuoteIds = excludedQuoteIds,
                limit = SEMANTIC_FALLBACK_LIMIT,
            )

        return candidateProvider.findCandidatesByQuoteIds(quoteIds)
    }

    private companion object {
        const val SEMANTIC_FALLBACK_LIMIT = 300
    }
}
