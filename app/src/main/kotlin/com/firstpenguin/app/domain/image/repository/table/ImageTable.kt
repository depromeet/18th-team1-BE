package com.firstpenguin.app.domain.image.repository.table

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object ImageTable {
    val IMAGES = DSL.table(DSL.name("images"))
    val ID = DSL.field(DSL.name("images", "id"), Long::class.java)
    val URL = DSL.field(DSL.name("images", "url"), String::class.java)
    val CREATED_AT = DSL.field(DSL.name("images", "created_at"), LocalDateTime::class.java)
}
