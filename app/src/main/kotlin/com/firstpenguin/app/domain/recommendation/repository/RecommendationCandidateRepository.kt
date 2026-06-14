package com.firstpenguin.app.domain.recommendation.repository

import com.firstpenguin.app.domain.book.repository.BookTable
import com.firstpenguin.app.domain.emotion.repository.table.TagTable
import com.firstpenguin.app.domain.quote.repository.QuoteTable
import com.firstpenguin.app.domain.quotemetadata.repository.table.QuoteMetadataTable
import com.firstpenguin.app.domain.quotemetadata.repository.table.QuoteMetadataTagTable
import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.global.enums.TagType
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.Table
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class RecommendationCandidateRepository(
    private val dsl: DSLContext,
) {
    fun findCandidates(
        effectiveTags: Collection<EffectiveTag>,
        limit: Int = DEFAULT_CANDIDATE_LIMIT,
    ): List<RecommendationCandidate> {
        val hardFilterTagIds = effectiveTags.hardFilterTagIds()
        if (hardFilterTagIds.isEmpty()) return emptyList()

        return dsl
            .select(CANDIDATE_ROW_FIELDS)
            .from(candidateQuoteIdsTable(hardFilterTagIds, limit))
            .join(QuoteTable.QUOTES)
            .on(QuoteTable.ID.eq(CANDIDATE_QUOTE_ID))
            .join(BookTable.BOOKS)
            .on(BookTable.ID.eq(QuoteTable.BOOK_ID))
            .join(QuoteMetadataTable.QUOTE_METADATA)
            .on(QuoteMetadataTable.QUOTE_ID.eq(QuoteTable.ID))
            .join(QuoteMetadataTagTable.QUOTE_METADATA_TAGS)
            .on(QuoteMetadataTagTable.QUOTE_METADATA_ID.eq(QuoteMetadataTable.ID))
            .join(TagTable.TAGS)
            .on(TagTable.ID.eq(QuoteMetadataTagTable.TAG_ID))
            .where(activeTagCondition())
            .orderBy(QuoteTable.ID.asc())
            .fetch()
            .toRecommendationCandidates()
    }

    private fun candidateQuoteIdsTable(
        hardFilterTagIds: List<Long>,
        limit: Int,
    ): Table<*> =
        dsl
            .select(QuoteTable.ID.`as`(CANDIDATE_QUOTE_ID_NAME))
            .from(QuoteTable.QUOTES)
            .join(BookTable.BOOKS)
            .on(BookTable.ID.eq(QuoteTable.BOOK_ID))
            .join(QuoteMetadataTable.QUOTE_METADATA)
            .on(QuoteMetadataTable.QUOTE_ID.eq(QuoteTable.ID))
            .where(activeQuoteAndBookCondition())
            .and(hardFilterMatchExists(hardFilterTagIds))
            .orderBy(QuoteTable.ID.asc())
            .limit(limit)
            .asTable(CANDIDATE_QUOTE_IDS_TABLE)

    private fun hardFilterMatchExists(hardFilterTagIds: List<Long>): Condition =
        DSL.exists(
            DSL
                .selectOne()
                .from(QuoteMetadataTagTable.QUOTE_METADATA_TAGS)
                .join(TagTable.TAGS)
                .on(TagTable.ID.eq(QuoteMetadataTagTable.TAG_ID))
                .where(QuoteMetadataTagTable.QUOTE_METADATA_ID.eq(QuoteMetadataTable.ID))
                .and(TagTable.ID.`in`(hardFilterTagIds))
                .and(TagTable.TYPE.`in`(HARD_FILTER_TAG_TYPE_NAMES))
                .and(activeTagCondition()),
        )

    private fun activeQuoteAndBookCondition(): Condition =
        QuoteTable.DELETED_AT
            .isNull
            .and(BookTable.DELETED_AT.isNull)

    private fun activeTagCondition(): Condition = TagTable.IS_ACTIVE.isTrue

    private fun Collection<EffectiveTag>.hardFilterTagIds(): List<Long> =
        filter { tag -> tag.type in HARD_FILTER_TAG_TYPES }
            .map { tag -> tag.tagId }
            .distinct()

    private fun List<Record>.toRecommendationCandidates(): List<RecommendationCandidate> =
        groupBy { record -> record[QuoteTable.ID]!! }
            .values
            .map { records -> records.toRecommendationCandidate() }

    private fun List<Record>.toRecommendationCandidate(): RecommendationCandidate {
        val firstRecord = first()
        val tagIdsByType = tagIdsByType()

        return RecommendationCandidate(
            quoteId = firstRecord[QuoteTable.ID]!!,
            bookId = firstRecord[QuoteTable.BOOK_ID]!!,
            content = firstRecord[QuoteTable.CONTENT]!!,
            title = firstRecord[BookTable.TITLE]!!,
            author = firstRecord[BookTable.AUTHOR]!!,
            roleTagId = tagIdsByType[TagType.ROLE]?.firstOrNull(),
            tagIdsByType = tagIdsByType,
        )
    }

    private fun List<Record>.tagIdsByType(): Map<TagType, Set<Long>> =
        groupBy { record -> TagType.from(record[TagTable.TYPE]!!) }
            .mapValues { (_, records) ->
                records
                    .map { record -> record[TagTable.ID]!! }
                    .toSet()
            }

    private companion object {
        const val DEFAULT_CANDIDATE_LIMIT = 300
        const val CANDIDATE_QUOTE_IDS_TABLE = "candidate_quote_ids"
        const val CANDIDATE_QUOTE_ID_NAME = "quote_id"
        val CANDIDATE_QUOTE_ID: Field<Long> =
            DSL.field(DSL.name(CANDIDATE_QUOTE_IDS_TABLE, CANDIDATE_QUOTE_ID_NAME), Long::class.java)
        val HARD_FILTER_TAG_TYPES = setOf(TagType.EMOTION, TagType.NEED)
        val HARD_FILTER_TAG_TYPE_NAMES = HARD_FILTER_TAG_TYPES.map { type -> type.name }
        val CANDIDATE_ROW_FIELDS: List<Field<*>> =
            listOf(
                QuoteTable.ID,
                QuoteTable.BOOK_ID,
                QuoteTable.CONTENT,
                BookTable.TITLE,
                BookTable.AUTHOR,
                TagTable.ID,
                TagTable.TYPE,
            )
    }
}
