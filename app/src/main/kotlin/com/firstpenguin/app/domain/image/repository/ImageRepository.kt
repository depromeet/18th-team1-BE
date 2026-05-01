package com.firstpenguin.app.domain.image.repository

import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ImageRepository(
    private val dsl: DSLContext,
) {
    fun findUrlById(id: Long): String? =
        dsl
            .select(ImageTable.URL)
            .from(ImageTable.IMAGES)
            .where(ImageTable.ID.eq(id))
            .fetchOne(ImageTable.URL)
}
