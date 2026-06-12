package com.firstpenguin.app.domain.quotecreation.review.repository

import com.firstpenguin.app.domain.book.model.Book
import com.firstpenguin.app.domain.book.repository.BookTable
import com.firstpenguin.app.domain.quote.repository.activeQuoteCountLessThanRecommended
import com.firstpenguin.app.domain.quotecreation.review.model.QuoteCandidate
import com.firstpenguin.app.domain.quotecreation.review.model.QuoteCandidateStatus
import com.firstpenguin.app.domain.quotecreation.review.model.QuoteReviewBatchTarget
import com.firstpenguin.app.domain.quotecreation.review.repository.table.QuoteCandidateTable
import com.firstpenguin.app.global.enums.QuoteConstants.REJECT_REASON_NOT_ACCEPTED
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

private val BOOK_FIELDS: List<Field<*>> =
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

private val CANDIDATE_FIELDS: List<Field<*>> =
    listOf(
        QuoteCandidateTable.ID,
        QuoteCandidateTable.BOOK_ID,
        QuoteCandidateTable.CONTENT,
        QuoteCandidateTable.SOURCE,
        QuoteCandidateTable.STATUS,
        QuoteCandidateTable.REJECT_REASON,
        QuoteCandidateTable.CREATED_AT,
        QuoteCandidateTable.UPDATED_AT,
    )

@Repository
class QuoteCandidateRepository(
    private val dsl: DSLContext,
) {
    fun findPendingTargets(limit: Int): List<QuoteReviewBatchTarget> {
        val books = findBooksWithPendingCandidates(limit)
        val candidates = findPendingCandidatesByBookIds(books.map { book -> book.id })
        return books
            .map { book -> book.toBatchTarget(candidates) }
            .filter { target -> target.candidates.isNotEmpty() }
    }

    fun findPendingCandidatesByBookId(bookId: Long): List<QuoteCandidate> =
        dsl
            .select(CANDIDATE_FIELDS)
            .from(QuoteCandidateTable.QUOTE_CANDIDATES)
            .where(QuoteCandidateTable.BOOK_ID.eq(bookId))
            .and(QuoteCandidateTable.STATUS.eq(QuoteCandidateStatus.PENDING.name))
            .fetch(::toCandidate)

    fun markCandidatesAccepted(candidateIds: List<Long>) {
        updateCandidateStatus(candidateIds, QuoteCandidateStatus.ACCEPTED, null)
    }

    fun markCandidatesRejected(candidateIds: List<Long>) =
        updateCandidateStatus(candidateIds, QuoteCandidateStatus.REJECTED, REJECT_REASON_NOT_ACCEPTED)

    private fun findBooksWithPendingCandidates(limit: Int): List<Book> =
        dsl
            .select(BOOK_FIELDS)
            .from(BookTable.BOOKS)
            .where(BookTable.DELETED_AT.isNull)
            .and(pendingCandidateExists())
            .and(activeQuoteCountLessThanRecommended(BookTable.ID))
            .orderBy(BookTable.ID.asc())
            .limit(limit)
            .fetch(::toBook)

    private fun findPendingCandidatesByBookIds(bookIds: List<Long>): List<QuoteCandidate> {
        if (bookIds.isEmpty()) return emptyList()
        return dsl
            .select(CANDIDATE_FIELDS)
            .from(QuoteCandidateTable.QUOTE_CANDIDATES)
            .where(QuoteCandidateTable.BOOK_ID.`in`(bookIds))
            .and(QuoteCandidateTable.STATUS.eq(QuoteCandidateStatus.PENDING.name))
            .orderBy(QuoteCandidateTable.BOOK_ID.asc(), QuoteCandidateTable.ID.asc())
            .fetch(::toCandidate)
    }

    private fun updateCandidateStatus(
        candidateIds: List<Long>,
        status: QuoteCandidateStatus,
        rejectReason: String?,
    ) {
        if (candidateIds.isEmpty()) return
        dsl
            .update(QuoteCandidateTable.QUOTE_CANDIDATES)
            .set(QuoteCandidateTable.STATUS, status.name)
            .set(QuoteCandidateTable.REJECT_REASON, rejectReason)
            .set(QuoteCandidateTable.UPDATED_AT, LocalDateTime.now())
            .where(QuoteCandidateTable.ID.`in`(candidateIds))
            .execute()
    }
}

private fun Book.toBatchTarget(candidates: List<QuoteCandidate>) =
    QuoteReviewBatchTarget(
        book = this,
        candidates = candidates.filter { candidate -> candidate.bookId == id },
    )

private fun pendingCandidateExists() =
    DSL.exists(
        DSL
            .selectOne()
            .from(QuoteCandidateTable.QUOTE_CANDIDATES)
            .where(QuoteCandidateTable.BOOK_ID.eq(BookTable.ID))
            .and(QuoteCandidateTable.STATUS.eq(QuoteCandidateStatus.PENDING.name)),
    )

private fun toCandidate(record: Record): QuoteCandidate =
    QuoteCandidate(
        id = record[QuoteCandidateTable.ID]!!,
        bookId = record[QuoteCandidateTable.BOOK_ID]!!,
        content = record[QuoteCandidateTable.CONTENT]!!,
        source = record[QuoteCandidateTable.SOURCE],
        status = QuoteCandidateStatus.valueOf(record[QuoteCandidateTable.STATUS]!!),
        rejectReason = record[QuoteCandidateTable.REJECT_REASON],
        createdAt = record[QuoteCandidateTable.CREATED_AT]!!,
        updatedAt = record[QuoteCandidateTable.UPDATED_AT]!!,
    )

private fun toBook(record: Record): Book =
    Book(
        id = record[BookTable.ID]!!,
        title = record[BookTable.TITLE]!!,
        author = record[BookTable.AUTHOR]!!,
        isbn13 = record[BookTable.ISBN13]!!,
        aladinLink = record[BookTable.ALADIN_LINK]!!,
        coverImageUrl = record[BookTable.COVER_IMAGE_URL]!!,
        createdAt = record[BookTable.CREATED_AT]!!,
        updatedAt = record[BookTable.UPDATED_AT]!!,
        deletedAt = record[BookTable.DELETED_AT],
    )
