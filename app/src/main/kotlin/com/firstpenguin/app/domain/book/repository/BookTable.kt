package com.firstpenguin.app.domain.book.repository

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object BookTable {
    val BOOKS = DSL.table(DSL.name("books"))
    val ID = DSL.field(DSL.name("books", "id"), Long::class.java)
    val TITLE = DSL.field(DSL.name("books", "title"), String::class.java)
    val AUTHOR = DSL.field(DSL.name("books", "author"), String::class.java)
    val ISBN13 = DSL.field(DSL.name("books", "isbn13"), String::class.java)
    val ALADIN_LINK = DSL.field(DSL.name("books", "aladin_link"), String::class.java)
    val COVER_IMAGE_URL = DSL.field(DSL.name("books", "cover_image_url"), String::class.java)
    val CREATED_AT = DSL.field(DSL.name("books", "created_at"), LocalDateTime::class.java)
    val UPDATED_AT = DSL.field(DSL.name("books", "updated_at"), LocalDateTime::class.java)
    val DELETED_AT = DSL.field(DSL.name("books", "deleted_at"), LocalDateTime::class.java)
}
