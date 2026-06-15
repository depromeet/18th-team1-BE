package com.firstpenguin.app.domain.recommendation.repository

import com.firstpenguin.app.domain.book.repository.BookTable
import com.firstpenguin.app.domain.emotion.repository.table.TagTable
import com.firstpenguin.app.domain.quote.repository.QuoteTable
import com.firstpenguin.app.domain.quotemetadata.repository.table.QuoteMetadataTable
import com.firstpenguin.app.domain.quotemetadata.repository.table.QuoteMetadataTagTable
import com.firstpenguin.app.global.enums.TagType
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class RecommendationTagRarityRepository(
    private val dsl: DSLContext,
) {
    fun findMetadataTagRarityWeights(): Map<Long, Double> {
        val tagCounts =
            dsl
                .select(TagTable.ID, TagTable.TYPE, TAG_ASSIGNMENT_COUNT)
                .from(QuoteMetadataTagTable.QUOTE_METADATA_TAGS)
                .join(QuoteMetadataTable.QUOTE_METADATA)
                .on(QuoteMetadataTable.ID.eq(QuoteMetadataTagTable.QUOTE_METADATA_ID))
                .join(QuoteTable.QUOTES)
                .on(QuoteTable.ID.eq(QuoteMetadataTable.QUOTE_ID))
                .join(BookTable.BOOKS)
                .on(BookTable.ID.eq(QuoteTable.BOOK_ID))
                .join(TagTable.TAGS)
                .on(TagTable.ID.eq(QuoteMetadataTagTable.TAG_ID))
                .where(TagTable.TYPE.`in`(RARITY_WEIGHTED_TAG_TYPE_NAMES))
                .and(TagTable.IS_ACTIVE.isTrue)
                .and(QuoteTable.DELETED_AT.isNull)
                .and(BookTable.DELETED_AT.isNull)
                .groupBy(TagTable.ID, TagTable.TYPE)
                .fetch(::toMetadataTagCount)

        return tagCounts.toRarityWeights()
    }

    private fun toMetadataTagCount(record: Record): MetadataTagCount =
        MetadataTagCount(
            tagId = record[TagTable.ID]!!,
            type = TagType.from(record[TagTable.TYPE]!!),
            count = record[TAG_ASSIGNMENT_COUNT]!!,
        )

    private fun List<MetadataTagCount>.toRarityWeights(): Map<Long, Double> {
        val totalCountByType = groupBy { count -> count.type }.mapValues { (_, counts) -> counts.sumOf { it.count } }

        return associate { count ->
            val totalCount = totalCountByType.getValue(count.type)
            count.tagId to rarityWeight(count.count.toDouble() / totalCount)
        }
    }

    private fun rarityWeight(typeRatio: Double): Double = 1.0 - typeRatio.coerceIn(MIN_TYPE_RATIO, MAX_DISCOUNT_RATIO)

    private companion object {
        const val TAG_ASSIGNMENT_COUNT_NAME = "tag_assignment_count"
        const val MIN_TYPE_RATIO = 0.0
        const val MAX_DISCOUNT_RATIO = 0.5

        val TAG_ASSIGNMENT_COUNT: Field<Int> = DSL.count().`as`(TAG_ASSIGNMENT_COUNT_NAME)
        val RARITY_WEIGHTED_TAG_TYPES = setOf(TagType.EMOTION, TagType.NEED, TagType.MOOD)
        val RARITY_WEIGHTED_TAG_TYPE_NAMES = RARITY_WEIGHTED_TAG_TYPES.map { type -> type.name }
    }
}

private data class MetadataTagCount(
    val tagId: Long,
    val type: TagType,
    val count: Int,
)
