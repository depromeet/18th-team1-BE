package com.firstpenguin.app.domain.image.repository.table

import org.jooq.impl.DSL
import kotlin.jvm.java

internal object ImageOwnerTable {
    val IMAGE_OWNERS = DSL.table(DSL.name("image_owners"))
    val IMAGE_ID = DSL.field(DSL.name("image_owners", "image_id"), Long::class.java)
    val OWNER_TYPE = DSL.field(DSL.name("image_owners", "owner_type"), String::class.java)
    val OWNER_ID = DSL.field(DSL.name("image_owners", "owner_id"), Long::class.java)
    val SORT_ORDER = DSL.field(DSL.name("image_owners", "sort_order"), Int::class.java)
}
