package com.firstpenguin.app.domain.diary.repository

import com.firstpenguin.app.domain.book.repository.BookTable
import com.firstpenguin.app.domain.diary.model.Diary
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
    ): List<Diary> =
        dsl
            .select(DIARY_JOIN_FIELDS)
            .from(DiaryTable.DIARIES)
            .join(QuoteTable.QUOTES)
            .on(DiaryTable.QUOTE_ID.eq(QuoteTable.ID))
            .join(BookTable.BOOKS)
            .on(QuoteTable.BOOK_ID.eq(BookTable.ID))
            .where(DiaryTable.USER_ID.eq(userId))
            .and(DiaryTable.CREATED_AT.ge(start))
            .and(DiaryTable.CREATED_AT.lt(end))
            .and(DiaryTable.DELETED_AT.isNull)
            .orderBy(DiaryTable.CREATED_AT.asc(), DiaryTable.ID.asc())
            .fetch(::toDiary)

    fun findById(id: Long): Diary? =
        dsl
            .select(DIARY_JOIN_FIELDS)
            .from(DiaryTable.DIARIES)
            .join(QuoteTable.QUOTES)
            .on(DiaryTable.QUOTE_ID.eq(QuoteTable.ID))
            .join(BookTable.BOOKS)
            .on(QuoteTable.BOOK_ID.eq(BookTable.ID))
            .where(DiaryTable.ID.eq(id))
            .and(DiaryTable.DELETED_AT.isNull)
            .fetchOne(::toDiary)

    fun updateContent(
        id: Long,
        userId: Long,
        content: String,
        start: LocalDateTime,
        end: LocalDateTime,
    ): Int =
        dsl
            .update(DiaryTable.DIARIES)
            .set(DiaryTable.CONTENT, content)
            .set(DiaryTable.UPDATED_AT, LocalDateTime.now())
            .where(DiaryTable.ID.eq(id))
            .and(DiaryTable.USER_ID.eq(userId))
            .and(DiaryTable.CREATED_AT.ge(start))
            .and(DiaryTable.CREATED_AT.lt(end))
            .and(DiaryTable.DELETED_AT.isNull)
            .execute()

    fun delete(
        id: Long,
        userId: Long,
        start: LocalDateTime,
        end: LocalDateTime,
    ): Int {
        val now = LocalDateTime.now()

        return dsl
            .update(DiaryTable.DIARIES)
            .set(DiaryTable.DELETED_AT, now)
            .set(DiaryTable.UPDATED_AT, now)
            .where(DiaryTable.ID.eq(id))
            .and(DiaryTable.USER_ID.eq(userId))
            .and(DiaryTable.CREATED_AT.ge(start))
            .and(DiaryTable.CREATED_AT.lt(end))
            .and(DiaryTable.DELETED_AT.isNull)
            .execute()
    }

    private fun toDiary(record: Record): Diary =
        Diary(
            id = record.get(DiaryTable.ID),
            userId = record.get(DiaryTable.USER_ID),
            quoteId = record.get(DiaryTable.QUOTE_ID),
            diaryImageId = record.get(DiaryTable.DIARY_IMAGE_ID),
            emotionIntensity = record.get(DiaryTable.EMOTION_INTENSITY),
            content = record.get(DiaryTable.CONTENT),
            createdAt = record.get(DiaryTable.CREATED_AT),
            updatedAt = record.get(DiaryTable.UPDATED_AT),
            deletedAt = record.get(DiaryTable.DELETED_AT),
            quoteContent = record.required(QuoteTable.CONTENT),
            coverImageUrl = record.required(BookTable.COVER_IMAGE_URL),
            author = record.required(BookTable.AUTHOR),
            title = record.required(BookTable.TITLE),
            aladinLink = record.required(BookTable.ALADIN_LINK),
        )

    private fun <T : Any> Record.required(field: Field<T>): T = get(field) ?: error("${field.qualifiedName} is null")

    private companion object {
        val DIARY_JOIN_FIELDS: List<Field<*>> =
            listOf(
                DiaryTable.ID,
                DiaryTable.USER_ID,
                DiaryTable.QUOTE_ID,
                DiaryTable.DIARY_IMAGE_ID,
                DiaryTable.EMOTION_INTENSITY,
                DiaryTable.CONTENT,
                DiaryTable.CREATED_AT,
                DiaryTable.UPDATED_AT,
                DiaryTable.DELETED_AT,
                QuoteTable.CONTENT,
                BookTable.COVER_IMAGE_URL,
                BookTable.AUTHOR,
                BookTable.TITLE,
                BookTable.ALADIN_LINK,
            )
    }
}
