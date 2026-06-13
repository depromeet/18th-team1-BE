package com.firstpenguin.app.domain.diary.repository

import com.firstpenguin.app.domain.book.repository.BookTable
import com.firstpenguin.app.domain.diary.model.Diary
import com.firstpenguin.app.domain.diary.repository.table.DiaryTable
import com.firstpenguin.app.domain.diary.repository.table.DiaryTagTable
import com.firstpenguin.app.domain.emotion.repository.table.TagTable
import com.firstpenguin.app.domain.quote.repository.QuoteTable
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class DiaryRepository(
    private val dsl: DSLContext,
) {
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

    private fun toDiary(record: Record): Diary =
        Diary(
            id = record.get(DiaryTable.ID),
            userId = record.get(DiaryTable.USER_ID),
            quoteId = record.get(DiaryTable.QUOTE_ID),
            emotionValue = record.get(DiaryTable.EMOTION_VALUE),
            content = record.get(DiaryTable.CONTENT),
            createdAt = record.get(DiaryTable.CREATED_AT),
            updatedAt = record.get(DiaryTable.UPDATED_AT),
            deletedAt = record.get(DiaryTable.DELETED_AT),
            quoteContent = record.required(QuoteTable.CONTENT),
            coverImageUrl = record.required(BookTable.COVER_IMAGE_URL),
            author = record.required(BookTable.AUTHOR),
            title = record.required(BookTable.TITLE),
            aladinLink = record.required(BookTable.ALADIN_LINK),
            tags = record.get(TAGS_FIELD).orEmpty(),
        )

    private fun <T : Any> Record.required(field: Field<T>): T = get(field) ?: error("${field.qualifiedName} is null")

    private companion object {
        private val TAGS_FIELD: Field<List<String>> =
            DSL
                .multiset(
                    DSL
                        .select(TagTable.LABEL)
                        .from(DiaryTagTable.DIARY_TAGS)
                        .join(TagTable.TAGS)
                        .on(DiaryTagTable.TAG_ID.eq(TagTable.ID))
                        .where(DiaryTagTable.DIARY_ID.eq(DiaryTable.ID))
                        .orderBy(DiaryTagTable.TAG_ID.asc()),
                ).convertFrom { records ->
                    records.map { record -> record.get(TagTable.LABEL) }
                }

        val DIARY_JOIN_FIELDS: List<Field<*>> =
            listOf(
                DiaryTable.ID,
                DiaryTable.USER_ID,
                DiaryTable.QUOTE_ID,
                DiaryTable.EMOTION_VALUE,
                DiaryTable.CONTENT,
                DiaryTable.CREATED_AT,
                DiaryTable.UPDATED_AT,
                DiaryTable.DELETED_AT,
                QuoteTable.CONTENT,
                BookTable.COVER_IMAGE_URL,
                BookTable.AUTHOR,
                BookTable.TITLE,
                BookTable.ALADIN_LINK,
                TAGS_FIELD,
            )
    }
}
