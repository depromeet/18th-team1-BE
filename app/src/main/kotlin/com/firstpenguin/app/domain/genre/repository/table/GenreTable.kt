package com.firstpenguin.app.domain.genre.repository.table

import org.jooq.impl.DSL

internal object GenreTable {
    val GENRES = DSL.table(DSL.name("genres"))
    val ID = DSL.field(DSL.name("genres", "id"), Long::class.java)
    val LABEL = DSL.field(DSL.name("genres", "label"), String::class.java)
    val SORT_ORDER = DSL.field(DSL.name("genres", "sort_order"), Int::class.java)
}
