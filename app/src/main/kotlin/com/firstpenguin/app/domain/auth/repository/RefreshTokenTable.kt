package com.firstpenguin.app.domain.auth.repository

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object RefreshTokenTable {
    val REFRESH_TOKENS = DSL.table(DSL.name("refresh_tokens"))
    val ID = DSL.field(DSL.name("refresh_tokens", "id"), Long::class.java)
    val USER_ID = DSL.field(DSL.name("refresh_tokens", "user_id"), Long::class.java)
    val DEVICE_ID = DSL.field(DSL.name("refresh_tokens", "device_id"), String::class.java)
    val TOKEN_HASH = DSL.field(DSL.name("refresh_tokens", "token_hash"), String::class.java)
    val EXPIRES_AT = DSL.field(DSL.name("refresh_tokens", "expires_at"), LocalDateTime::class.java)
    val CREATED_AT = DSL.field(DSL.name("refresh_tokens", "created_at"), LocalDateTime::class.java)
    val UPDATED_AT = DSL.field(DSL.name("refresh_tokens", "updated_at"), LocalDateTime::class.java)
}
