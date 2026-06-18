package com.firstpenguin.app.domain.user.repository

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object OAuthAccountTable {
    val OAUTH_ACCOUNTS = DSL.table(DSL.name("oauth_accounts"))
    val ID = DSL.field(DSL.name("oauth_accounts", "id"), Long::class.java)
    val USER_ID = DSL.field(DSL.name("oauth_accounts", "user_id"), Long::class.java)
    val PROVIDER = DSL.field(DSL.name("oauth_accounts", "provider"), String::class.java)
    val PROVIDER_ID = DSL.field(DSL.name("oauth_accounts", "provider_id"), String::class.java)
    val EMAIL = DSL.field(DSL.name("oauth_accounts", "email"), String::class.java)
    val PROVIDER_DISPLAY_NAME = DSL.field(DSL.name("oauth_accounts", "provider_display_name"), String::class.java)
    val LAST_LOGIN_AT = DSL.field(DSL.name("oauth_accounts", "last_login_at"), LocalDateTime::class.java)
    val DISCONNECTED_AT = DSL.field(DSL.name("oauth_accounts", "disconnected_at"), LocalDateTime::class.java)
    val CREATED_AT = DSL.field(DSL.name("oauth_accounts", "created_at"), LocalDateTime::class.java)
    val UPDATED_AT = DSL.field(DSL.name("oauth_accounts", "updated_at"), LocalDateTime::class.java)
}
