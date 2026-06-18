package com.firstpenguin.app.domain.genre.repository

import com.firstpenguin.app.domain.genre.model.Genre
import com.firstpenguin.app.domain.genre.repository.table.GenreTable
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository

@Repository
class GenreRepository(
    private val dsl: DSLContext,
) {
    fun findAll(): List<Genre> =
        dsl
            .select(GenreTable.ID, GenreTable.LABEL, GenreTable.SORT_ORDER)
            .from(GenreTable.GENRES)
            .orderBy(GenreTable.SORT_ORDER.asc(), GenreTable.ID.asc())
            .fetch(::toGenre)

    private fun toGenre(record: Record): Genre =
        Genre(
            id = record[GenreTable.ID]!!,
            label = record[GenreTable.LABEL]!!,
            sortOrder = record[GenreTable.SORT_ORDER]!!,
        )
}
