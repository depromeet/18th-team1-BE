package com.firstpenguin.app.domain.embedding.repository

import com.firstpenguin.app.domain.embedding.model.OpenAiEmbeddingModelVersion
import com.firstpenguin.app.domain.embedding.model.QuoteEmbedding
import com.firstpenguin.app.domain.embedding.model.QuoteEmbeddingTarget
import com.firstpenguin.app.domain.embedding.repository.table.QuoteEmbeddingTable
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchType
import com.firstpenguin.app.domain.quotebatch.repository.table.QuoteBatchItemTable
import com.firstpenguin.app.domain.quotemetadata.repository.table.QuoteMetadataTable
import com.firstpenguin.app.global.enums.BatchItemStatus
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Query
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class QuoteEmbeddingRepository(
    private val dsl: DSLContext,
) {
    fun findCosineSimilarities(
        quoteIds: Collection<Long>,
        userEmbedding: List<Double>,
        embeddingModel: String = OpenAiEmbeddingModelVersion.V1.model,
    ): Map<Long, Double> {
        if (quoteIds.isEmpty()) return emptyMap()
        val semanticScore =
            DSL
                .field(
                    "1 - ({0} <=> {1})",
                    Double::class.java,
                    QuoteEmbeddingTable.EMBEDDING,
                    vectorField(userEmbedding),
                ).`as`(SEMANTIC_SCORE_ALIAS)

        return dsl
            .select(QuoteEmbeddingTable.QUOTE_ID, semanticScore)
            .from(QuoteEmbeddingTable.QUOTE_EMBEDDINGS)
            .where(QuoteEmbeddingTable.QUOTE_ID.`in`(quoteIds.distinct()))
            .and(QuoteEmbeddingTable.EMBEDDING_MODEL.eq(embeddingModel))
            .fetchMap(QuoteEmbeddingTable.QUOTE_ID, semanticScore)
    }

    fun findMostSimilarQuoteIds(
        userEmbedding: List<Double>,
        excludedQuoteIds: Collection<Long>,
        limit: Int,
        embeddingModel: String = OpenAiEmbeddingModelVersion.V1.model,
    ): List<Long> {
        if (limit <= MIN_SIMILAR_QUOTE_LIMIT) return emptyList()
        val excludedIds = excludedQuoteIds.distinct()
        val semanticScore =
            DSL
                .field(
                    "1 - ({0} <=> {1})",
                    Double::class.java,
                    QuoteEmbeddingTable.EMBEDDING,
                    vectorField(userEmbedding),
                )

        return dsl
            .select(QuoteEmbeddingTable.QUOTE_ID)
            .from(QuoteEmbeddingTable.QUOTE_EMBEDDINGS)
            .where(QuoteEmbeddingTable.EMBEDDING_MODEL.eq(embeddingModel))
            .and(
                if (excludedIds.isEmpty()) {
                    DSL.trueCondition()
                } else {
                    QuoteEmbeddingTable.QUOTE_ID.notIn(excludedIds)
                },
            ).orderBy(semanticScore.desc(), QuoteEmbeddingTable.QUOTE_ID.asc())
            .limit(limit)
            .fetch(QuoteEmbeddingTable.QUOTE_ID)
    }

    fun findMetadataTargetsByJobId(
        jobId: Long,
        embeddingModel: String,
    ): List<QuoteEmbeddingTarget> =
        dsl
            .select(TARGET_FIELDS)
            .from(QuoteMetadataTable.QUOTE_METADATA)
            .join(QuoteBatchItemTable.QUOTE_BATCH_ITEMS)
            .on(metadataBatchItemJoinCondition())
            .leftJoin(QuoteEmbeddingTable.QUOTE_EMBEDDINGS)
            .on(quoteEmbeddingJoinCondition(embeddingModel))
            .where(batchJobCondition(jobId))
            .fetch(::toQuoteEmbeddingTarget)

    fun upsertQuoteEmbeddings(quoteEmbeddings: List<QuoteEmbedding>) {
        if (quoteEmbeddings.isEmpty()) return
        val now = LocalDateTime.now()
        val queries = quoteEmbeddings.map { quoteEmbedding -> upsertQuery(quoteEmbedding, now) }
        dsl.batch(queries).execute()
    }

    private fun upsertQuery(
        quoteEmbedding: QuoteEmbedding,
        now: LocalDateTime,
    ): Query =
        dsl
            .insertInto(QuoteEmbeddingTable.QUOTE_EMBEDDINGS)
            .set(QuoteEmbeddingTable.QUOTE_ID, quoteEmbedding.quoteId)
            .set(QuoteEmbeddingTable.EMBEDDING_MODEL, quoteEmbedding.embeddingModel)
            .set(QuoteEmbeddingTable.EMBEDDING, vectorField(quoteEmbedding.embedding))
            .set(QuoteEmbeddingTable.EMBEDDING_TEXT_HASH, quoteEmbedding.embeddingTextHash)
            .set(QuoteEmbeddingTable.CREATED_AT, now)
            .set(QuoteEmbeddingTable.UPDATED_AT, now)
            .onConflict(QuoteEmbeddingTable.QUOTE_ID, QuoteEmbeddingTable.EMBEDDING_MODEL)
            .doUpdate()
            .set(QuoteEmbeddingTable.EMBEDDING, vectorField(quoteEmbedding.embedding))
            .set(QuoteEmbeddingTable.EMBEDDING_TEXT_HASH, quoteEmbedding.embeddingTextHash)
            .set(QuoteEmbeddingTable.UPDATED_AT, now)

    private fun vectorField(embedding: List<Double>): Field<String> {
        val dimension = OpenAiEmbeddingModelVersion.V1.dimension

        if (embedding.size != dimension) {
            throw CustomException(ErrorCode.QUOTE_EMBEDDING_DIMENSION_MISMATCH)
        }

        return DSL.field(
            "?::vector($dimension)",
            String::class.java,
            embedding.toVectorLiteral(),
        )
    }

    private fun List<Double>.toVectorLiteral(): String =
        joinToString(
            prefix = VECTOR_PREFIX,
            postfix = VECTOR_POSTFIX,
        )

    private fun metadataBatchItemJoinCondition(): Condition = BATCH_ITEM_QUOTE_ID.eq(METADATA_QUOTE_ID)

    private fun quoteEmbeddingJoinCondition(embeddingModel: String): Condition =
        QuoteEmbeddingTable.QUOTE_ID
            .eq(QuoteMetadataTable.QUOTE_ID)
            .and(QuoteEmbeddingTable.EMBEDDING_MODEL.eq(embeddingModel))

    private fun batchJobCondition(jobId: Long): Condition =
        QuoteBatchItemTable.JOB_ID
            .eq(jobId)
            .and(QuoteBatchItemTable.JOB_TYPE.eq(QuoteBatchType.QUOTE_METADATA.name))
            .and(QuoteBatchItemTable.STATUS.eq(BatchItemStatus.SUCCEEDED.name))

    private fun toQuoteEmbeddingTarget(record: Record): QuoteEmbeddingTarget =
        QuoteEmbeddingTarget(
            quoteId = record[QuoteMetadataTable.QUOTE_ID]!!,
            embeddingText = record[QuoteMetadataTable.EMBEDDING_TEXT]!!,
            existingEmbeddingTextHash = record[EXISTING_EMBEDDING_TEXT_HASH],
        )

    private companion object {
        private const val MIN_SIMILAR_QUOTE_LIMIT = 0
        private const val VECTOR_PREFIX = "["
        private const val VECTOR_POSTFIX = "]"
        private const val SEMANTIC_SCORE_ALIAS = "semantic_score"
        private const val EXISTING_EMBEDDING_TEXT_HASH_ALIAS = "existing_embedding_text_hash"
        private val BATCH_ITEM_QUOTE_ID = QuoteBatchItemTable.TARGET_ID
        private val METADATA_QUOTE_ID = QuoteMetadataTable.QUOTE_ID
        private val EXISTING_EMBEDDING_TEXT_HASH =
            QuoteEmbeddingTable.EMBEDDING_TEXT_HASH.`as`(EXISTING_EMBEDDING_TEXT_HASH_ALIAS)
        private val TARGET_FIELDS: List<Field<*>> =
            listOf(
                QuoteMetadataTable.QUOTE_ID,
                QuoteMetadataTable.EMBEDDING_TEXT,
                EXISTING_EMBEDDING_TEXT_HASH,
            )
    }
}
