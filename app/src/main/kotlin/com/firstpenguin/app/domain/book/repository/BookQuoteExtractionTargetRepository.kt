package com.firstpenguin.app.domain.book.repository

import com.firstpenguin.app.domain.book.model.Book
import com.firstpenguin.app.domain.quote.repository.QuoteTable
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchType
import com.firstpenguin.app.domain.quotebatch.repository.table.QuoteBatchItemTable
import com.firstpenguin.app.domain.quotebatch.repository.table.QuoteBatchJobTable
import com.firstpenguin.app.domain.quotecreation.review.model.QuoteCandidateStatus
import com.firstpenguin.app.domain.quotecreation.review.repository.table.QuoteCandidateTable
import com.firstpenguin.app.global.enums.BatchItemStatus
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Select
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

private const val RECOMMENDED_QUOTE_COUNT = 3

@Repository
class BookQuoteExtractionTargetRepository(
    private val dsl: DSLContext,
) {
    fun findBooksNeedingQuotes(
        limit: Int,
        extractionModel: String,
        extractionVersion: Int,
    ): List<Book> =
        dsl
            .select(BOOK_FIELDS)
            .from(BookTable.BOOKS)
            .where(targetBookCondition(extractionModel, extractionVersion))
            .orderBy(BookTable.ID.asc())
            .limit(limit)
            .fetch(::toBook)

    fun countBooksNeedingQuotes(
        extractionModel: String,
        extractionVersion: Int,
    ): Int =
        dsl
            .selectCount()
            .from(BookTable.BOOKS)
            .where(targetBookCondition(extractionModel, extractionVersion))
            .fetchOne(0, Int::class.java) ?: 0

    private fun targetBookCondition(
        extractionModel: String,
        extractionVersion: Int,
    ): Condition =
        BookTable.DELETED_AT.isNull
            .and(activeQuoteCountLessThanRecommended())
            .and(noPendingCandidateExists())
            .and(noSucceededQuoteExtractionAttempt(extractionModel, extractionVersion))

    private fun toBook(record: Record): Book =
        Book(
            id = record.get(BookTable.ID),
            title = record.get(BookTable.TITLE),
            author = record.get(BookTable.AUTHOR),
            isbn13 = record.get(BookTable.ISBN13),
            aladinLink = record.get(BookTable.ALADIN_LINK),
            coverImageUrl = record.get(BookTable.COVER_IMAGE_URL),
            createdAt = record.get(BookTable.CREATED_AT),
            updatedAt = record.get(BookTable.UPDATED_AT),
            deletedAt = record.get(BookTable.DELETED_AT),
        )

    private fun activeQuoteCountLessThanRecommended() = activeQuoteCount().lessThan(RECOMMENDED_QUOTE_COUNT)

    private fun activeQuoteCount(): Field<Int> = DSL.field(activeQuoteCountSelect())

    private fun noSucceededQuoteExtractionAttempt(
        extractionModel: String,
        extractionVersion: Int,
    ): Condition = DSL.notExists(succeededQuoteExtractionAttemptSelect(extractionModel, extractionVersion))

    private fun activeQuoteCountSelect(): Select<Record1<Int>> =
        DSL
            .selectCount()
            .from(QuoteTable.QUOTES)
            .where(QuoteTable.BOOK_ID.eq(BookTable.ID))
            .and(QuoteTable.DELETED_AT.isNull)

    private fun succeededQuoteExtractionAttemptSelect(
        extractionModel: String,
        extractionVersion: Int,
    ): Select<Record1<Int>> =
        DSL
            .selectOne()
            .from(QuoteBatchItemTable.QUOTE_BATCH_ITEMS)
            .join(QuoteBatchJobTable.QUOTE_BATCH_JOBS)
            .on(QuoteBatchJobTable.ID.eq(QuoteBatchItemTable.JOB_ID))
            .where(succeededQuoteExtractionAttemptCondition(extractionModel, extractionVersion))

    private fun succeededQuoteExtractionAttemptCondition(
        extractionModel: String,
        extractionVersion: Int,
    ): Condition =
        QuoteBatchItemTable.TARGET_ID
            .eq(BookTable.ID)
            .and(QuoteBatchItemTable.STATUS.eq(BatchItemStatus.SUCCEEDED.name))
            .and(QuoteBatchJobTable.JOB_TYPE.eq(QuoteBatchType.QUOTE_EXTRACTION.name))
            .and(QuoteBatchJobTable.MODEL.eq(extractionModel))
            .and(QuoteBatchJobTable.VERSION.eq(extractionVersion))

    private companion object {
        val BOOK_FIELDS: List<Field<*>> =
            listOf(
                BookTable.ID,
                BookTable.TITLE,
                BookTable.AUTHOR,
                BookTable.ISBN13,
                BookTable.ALADIN_LINK,
                BookTable.COVER_IMAGE_URL,
                BookTable.CREATED_AT,
                BookTable.UPDATED_AT,
                BookTable.DELETED_AT,
            )
    }
}

private fun noPendingCandidateExists(): Condition =
    DSL.notExists(
        DSL
            .selectOne()
            .from(QuoteCandidateTable.QUOTE_CANDIDATES)
            .where(QuoteCandidateTable.BOOK_ID.eq(BookTable.ID))
            .and(QuoteCandidateTable.STATUS.eq(QuoteCandidateStatus.PENDING.name)),
    )
