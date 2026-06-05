package com.firstpenguin.app.domain.embedding.repository

import com.firstpenguin.app.domain.embedding.model.QuoteEmbedding
import com.firstpenguin.app.domain.embedding.model.QuoteEmbeddingTarget
import com.firstpenguin.app.domain.embedding.repository.table.QuoteEmbeddingTable
import com.firstpenguin.app.domain.quotemetadata.repository.table.QuoteMetadataBatchItemTable
import com.firstpenguin.app.domain.quotemetadata.repository.table.QuoteMetadataTable
import com.firstpenguin.app.global.enums.BatchItemStatus
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
    fun findMetadataTargetsByJobId(
        jobId: Long,
        embeddingModel: String,
    ): List<QuoteEmbeddingTarget> =
        dsl
            .select(TARGET_FIELDS)
            .from(QuoteMetadataTable.QUOTE_METADATA)
            .join(QuoteMetadataBatchItemTable.QUOTE_METADATA_BATCH_ITEMS)
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

    private fun vectorField(embedding: List<Double>): Field<String> =
        DSL.field(
            "?::vector",
            String::class.java,
            embedding.toVectorLiteral(),
        )

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
        QuoteMetadataBatchItemTable.JOB_ID
            .eq(jobId)
            .and(QuoteMetadataBatchItemTable.STATUS.eq(BatchItemStatus.SUCCEEDED.name))

    private fun toQuoteEmbeddingTarget(record: Record): QuoteEmbeddingTarget =
        QuoteEmbeddingTarget(
            quoteId = record[QuoteMetadataTable.QUOTE_ID]!!,
            embeddingText = record[QuoteMetadataTable.EMBEDDING_TEXT]!!,
            existingEmbeddingTextHash = record[EXISTING_EMBEDDING_TEXT_HASH],
        )

    private companion object {
        private const val VECTOR_PREFIX = "["
        private const val VECTOR_POSTFIX = "]"
        private const val EXISTING_EMBEDDING_TEXT_HASH_ALIAS = "existing_embedding_text_hash"
        private val BATCH_ITEM_QUOTE_ID = QuoteMetadataBatchItemTable.QUOTE_ID
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
