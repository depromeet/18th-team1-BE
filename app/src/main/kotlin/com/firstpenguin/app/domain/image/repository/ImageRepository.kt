package com.firstpenguin.app.domain.image.repository

import com.firstpenguin.app.domain.image.repository.table.ImageOwnerTable
import com.firstpenguin.app.domain.image.repository.table.ImageTable
import com.firstpenguin.app.global.enums.ImageOwner
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

    fun findUrlsByOwnerTypeAndOwnerId(ownerType: ImageOwner, ownerId: Long): List<String> =
        dsl
            .select(ImageTable.URL)
            .from(ImageTable.IMAGES)
            .join(ImageOwnerTable.IMAGE_OWNERS)
            .on(ImageTable.ID.eq(ImageOwnerTable.IMAGE_ID))
            .where(ImageOwnerTable.OWNER_TYPE.eq(ownerType.name))
                .and(ImageOwnerTable.OWNER_ID.eq(ownerId))
            .fetch(ImageTable.URL)
}
