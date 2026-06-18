package com.firstpenguin.app.domain.user.repository

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object UserTable {
    val USERS = DSL.table(DSL.name("users"))
    val ID = DSL.field(DSL.name("users", "id"), Long::class.java)
    val NICKNAME = DSL.field(DSL.name("users", "nickname"), String::class.java)
    val PROFILE_IMAGE_ID = DSL.field(DSL.name("users", "profile_image_id"), Long::class.java)
    val STATUS = DSL.field(DSL.name("users", "status"), String::class.java)
    val DELETED_AT = DSL.field(DSL.name("users", "deleted_at"), LocalDateTime::class.java)
    val CREATED_AT = DSL.field(DSL.name("users", "created_at"), LocalDateTime::class.java)
    val UPDATED_AT = DSL.field(DSL.name("users", "updated_at"), LocalDateTime::class.java)
}
