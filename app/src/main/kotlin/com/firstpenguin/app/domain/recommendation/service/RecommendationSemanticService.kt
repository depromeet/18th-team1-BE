package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.embedding.repository.QuoteEmbeddingRepository
import com.firstpenguin.app.domain.embedding.usecase.UserQueryEmbeddingProcessor
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.UserSemanticEmbedding
import com.firstpenguin.app.domain.recommendation.repository.RecommendationCandidateProvider
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(javaClass)

    override fun prepare(input: RecommendationInput): UserSemanticEmbedding? {
        val embeddingInput =
            log.measureRecommendationStep("semantic.buildEmbeddingInput", { "userId=${input.userId}" }) {
                userEmbeddingInputBuilder.build(input)
            } ?: return null

        return UserSemanticEmbedding(
            inputText = embeddingInput,
            embedding =
                log.measureRecommendationStep("semantic.openAiEmbedding", { "userId=${input.userId}" }) {
                    userQueryEmbeddingProcessor.embed(embeddingInput)
                },
        )
    }

    override fun findScores(
        userEmbedding: UserSemanticEmbedding?,
        quoteIds: Collection<Long>,
    ): Map<Long, Double> {
        if (userEmbedding == null) return emptyMap()
        var scoreCount = 0
        lateinit var scores: Map<Long, Double>

        log.measureRecommendationStep("semantic.findScores", { "quoteCount=${quoteIds.size} scoreCount=$scoreCount" }) {
            scores =
                quoteEmbeddingRepository.findCosineSimilarities(
                    quoteIds = quoteIds,
                    userEmbedding = userEmbedding.embedding,
                )
            scoreCount = scores.size
        }

        return scores
    }

    override fun findSimilarCandidates(
        userEmbedding: UserSemanticEmbedding?,
        excludedQuoteIds: Collection<Long>,
    ): List<RecommendationCandidate> {
        if (userEmbedding == null) return emptyList()
        var quoteIdCount = 0
        var candidateCount = 0
        lateinit var quoteIds: List<Long>
        lateinit var candidates: List<RecommendationCandidate>

        log.measureRecommendationStep("semantic.findSimilarQuoteIds", { "quoteCount=$quoteIdCount" }) {
            quoteIds =
                quoteEmbeddingRepository.findMostSimilarQuoteIds(
                    userEmbedding = userEmbedding.embedding,
                    excludedQuoteIds = excludedQuoteIds,
                    limit = SEMANTIC_FALLBACK_LIMIT,
                )
            quoteIdCount = quoteIds.size
        }

        log.measureRecommendationStep("semantic.findSimilarCandidates", { "candidateCount=$candidateCount" }) {
            candidates = candidateProvider.findCandidatesByQuoteIds(quoteIds)
            candidateCount = candidates.size
        }

        return candidates
    }

    private companion object {
        const val SEMANTIC_FALLBACK_LIMIT = 300
    }
}
