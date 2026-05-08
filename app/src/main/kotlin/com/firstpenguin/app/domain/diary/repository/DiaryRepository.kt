package com.firstpenguin.app.domain.diary.repository

import com.firstpenguin.app.domain.book.repository.BookTable
import com.firstpenguin.app.domain.diary.model.CreatedDiary
import com.firstpenguin.app.domain.diary.model.Diary
import com.firstpenguin.app.domain.diary.repository.table.DiaryTable
import com.firstpenguin.app.domain.diary.repository.table.DiaryTagTable
import com.firstpenguin.app.domain.emotion.repository.table.TagTable
import com.firstpenguin.app.domain.image.repository.table.ImageOwnerTable
import com.firstpenguin.app.domain.image.repository.table.ImageTable
import com.firstpenguin.app.domain.quote.repository.QuoteTable
import com.firstpenguin.app.global.enums.ImageOwner
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
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

    fun findDiaryImageUrlByDiaryId(id: Long): String? =
        dsl
            .select(ImageTable.URL)
            .from(ImageOwnerTable.IMAGE_OWNERS)
            .join(ImageTable.IMAGES)
            .on(ImageOwnerTable.IMAGE_ID.eq(ImageTable.ID))
            .where(ImageOwnerTable.OWNER_TYPE.eq(ImageOwner.DIARY.name))
            .and(ImageOwnerTable.OWNER_ID.eq(id))
            .orderBy(ImageOwnerTable.SORT_ORDER.asc(), ImageOwnerTable.IMAGE_ID.asc())
            .limit(1)
            .fetchOne(ImageTable.URL)

    fun existsByUserIdAndCreatedAtBetween(
        userId: Long,
        start: LocalDateTime,
        end: LocalDateTime,
    ): Boolean =
        dsl.fetchExists(
            DiaryTable.DIARIES,
            DiaryTable.USER_ID
                .eq(userId)
                .and(DiaryTable.CREATED_AT.ge(start))
                .and(DiaryTable.CREATED_AT.lt(end))
                .and(DiaryTable.DELETED_AT.isNull),
        )

    fun updateContent(
        id: Long,
        userId: Long,
        content: String?,
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

    fun countByUserId(userId: Long): Int =
        dsl
            .selectCount()
            .from(DiaryTable.DIARIES)
            .where(DiaryTable.USER_ID.eq(userId))
            .and(DiaryTable.DELETED_AT.isNull)
            .fetchOne(0, Int::class.java) ?: 0

    fun create(
        userId: Long,
        emotionIntensity: Int,
        quoteId: Long,
        content: String?,
    ): CreatedDiary =
        dsl
            .insertInto(DiaryTable.DIARIES)
            .set(DiaryTable.USER_ID, userId)
            .set(DiaryTable.EMOTION_INTENSITY, emotionIntensity)
            .set(DiaryTable.QUOTE_ID, quoteId)
            .set(DiaryTable.CONTENT, content)
            .returning(DiaryTable.ID, DiaryTable.CREATED_AT)
            .fetchOne()
            ?.let { record ->
                CreatedDiary(
                    diaryId = record.get(DiaryTable.ID),
                    createdAt = record.get(DiaryTable.CREATED_AT),
                )
            }
            ?: throw CustomException(ErrorCode.DIARY_CREATION_FAILED)

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
                TAGS_FIELD,
            )
    }
}
