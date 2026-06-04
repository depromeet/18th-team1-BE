package com.firstpenguin.app.domain.embedding.usecase

import com.firstpenguin.app.domain.embedding.model.QuoteEmbedding
import com.firstpenguin.app.domain.embedding.model.QuoteEmbeddingTarget
import com.firstpenguin.app.domain.embedding.repository.QuoteEmbeddingRepository
import com.firstpenguin.app.domain.embedding.service.OpenAiEmbeddingClient
import com.firstpenguin.app.global.enums.QuoteEmbeddingModelVersion
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.HexFormat

private const val QUOTE_EMBEDDING_CHUNK_SIZE = 100
private const val SHA_256_ALGORITHM = "SHA-256"

@Component
class QuoteEmbeddingBulkProcessor(
    private val quoteEmbeddingRepository: QuoteEmbeddingRepository,
    private val quoteEmbeddingCommandUseCase: QuoteEmbeddingCommandUseCase,
    private val openAiEmbeddingClient: OpenAiEmbeddingClient,
) {
    fun embedMetadataByJobId(jobId: Long): Int {
        val targets = findEmbeddingTargets(jobId)
        targets.chunked(QUOTE_EMBEDDING_CHUNK_SIZE).forEach { chunk -> embedChunk(chunk) }
        return targets.size
    }

    private fun findEmbeddingTargets(jobId: Long): List<QuoteEmbeddingTarget> =
        quoteEmbeddingRepository
            .findMetadataTargetsByJobId(jobId, QuoteEmbeddingModelVersion.V1.model)
            .filter { target -> target.needsEmbedding() }

    private fun embedChunk(targets: List<QuoteEmbeddingTarget>) {
        val embeddings = openAiEmbeddingClient.createEmbeddings(targets.map { target -> target.embeddingText })
        validateEmbeddingCount(targets, embeddings)
        quoteEmbeddingCommandUseCase.saveQuoteEmbeddings(targets.toQuoteEmbeddings(embeddings))
    }

    private fun validateEmbeddingCount(
        targets: List<QuoteEmbeddingTarget>,
        embeddings: List<List<Double>>,
    ) {
        if (targets.size != embeddings.size) {
            throw CustomException(ErrorCode.QUOTE_EMBEDDING_RESPONSE_SIZE_MISMATCH)
        }
    }

    private fun List<QuoteEmbeddingTarget>.toQuoteEmbeddings(embeddings: List<List<Double>>): List<QuoteEmbedding> =
        zip(embeddings).map { (target, embedding) -> target.toQuoteEmbedding(embedding) }

    private fun QuoteEmbeddingTarget.toQuoteEmbedding(embedding: List<Double>): QuoteEmbedding =
        QuoteEmbedding(
            quoteId = quoteId,
            embeddingModel = QuoteEmbeddingModelVersion.V1.model,
            embedding = embedding,
            embeddingTextHash = embeddingTextHash(),
        )

    private fun QuoteEmbeddingTarget.needsEmbedding(): Boolean = existingEmbeddingTextHash != embeddingTextHash()

    private fun QuoteEmbeddingTarget.embeddingTextHash(): String = sha256(embeddingText)

    private fun sha256(value: String): String {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        val digest = MessageDigest.getInstance(SHA_256_ALGORITHM).digest(bytes)
        return HexFormat.of().formatHex(digest)
    }
}
