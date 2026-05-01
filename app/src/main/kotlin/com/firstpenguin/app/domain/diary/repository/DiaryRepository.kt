package com.firstpenguin.app.domain.diary.repository

import com.firstpenguin.app.domain.book.repository.BookTable
import com.firstpenguin.app.domain.diary.model.DiarySummary
import com.firstpenguin.app.domain.quote.repository.QuoteTable
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DiaryRepository(
    private val dsl: DSLContext,
) {
    fun findAllByUserIdAndCreatedAtBetween(
        userId: Long,
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<DiarySummary> =
        dsl
            .select(DIARY_SUMMARY_FIELDS)
            .from(DiaryTable.DIARIES)
            .join(QuoteTable.QUOTES)
            .on(DiaryTable.QUOTE_ID.eq(QuoteTable.ID))
            .join(BookTable.BOOKS)
            .on(QuoteTable.BOOK_ID.eq(BookTable.ID))
            .where(DiaryTable.USER_ID.eq(userId))
            .and(DiaryTable.CREATED_AT.ge(start))
            .and(DiaryTable.CREATED_AT.lt(end))
            .and(DiaryTable.DELETED_AT.isNull)
            .and(QuoteTable.DELETED_AT.isNull)
            .and(BookTable.DELETED_AT.isNull)
            .orderBy(DiaryTable.CREATED_AT.asc(), DiaryTable.ID.asc())
            .fetch(::toDiarySummary)

    private fun toDiarySummary(record: Record): DiarySummary =
        DiarySummary(
            id = record.get(DiaryTable.ID),
            createdAt = record.get(DiaryTable.CREATED_AT).toLocalDate(),
            content = record.get(DiaryTable.CONTENT),
            emotionIntensity = record.get(DiaryTable.EMOTION_INTENSITY),
            quoteContent = record.get(QuoteTable.CONTENT),
            coverImageUrl = record.get(BookTable.COVER_IMAGE_URL),
            author = record.get(BookTable.AUTHOR),
            title = record.get(BookTable.TITLE),
        )

    private companion object {
        val DIARY_SUMMARY_FIELDS: List<Field<*>> =
            listOf(
                DiaryTable.ID,
                DiaryTable.CREATED_AT,
                DiaryTable.CONTENT,
                DiaryTable.EMOTION_INTENSITY,
                QuoteTable.CONTENT,
                BookTable.COVER_IMAGE_URL,
                BookTable.AUTHOR,
                BookTable.TITLE,
            )
    }
}
