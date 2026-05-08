package com.firstpenguin.app.domain.user.repository

import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.Provider
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.model.UserStatus
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class UserRepository(
    private val dsl: DSLContext,
) {
    fun findById(id: Long): User? =
        dsl
            .select(USER_FIELDS)
            .from(UserTable.USERS)
            .where(UserTable.ID.eq(id))
            .fetchOne(::toUser)

    fun upsertOAuthUser(profile: OAuthUserProfile): User {
        val now = LocalDateTime.now()

        return dsl
            .insertInto(UserTable.USERS)
            .set(UserTable.PROVIDER, profile.provider.name)
            .set(UserTable.PROVIDER_ID, profile.providerId)
            .set(UserTable.EMAIL, profile.email)
            .set(UserTable.NICKNAME, profile.nickname)
            .set(UserTable.STATUS, UserStatus.ACTIVE.name)
            .set(UserTable.LAST_LOGIN_AT, now)
            .set(UserTable.CREATED_AT, now)
            .set(UserTable.UPDATED_AT, now)
            .onConflict(PROVIDER_CONFLICT_TARGET, PROVIDER_ID_CONFLICT_TARGET)
            .doUpdate()
            .set(UserTable.EMAIL, DSL.coalesce(DSL.excluded(UserTable.EMAIL), UserTable.EMAIL))
            .set(UserTable.NICKNAME, DSL.excluded(UserTable.NICKNAME))
            .set(UserTable.STATUS, UserStatus.ACTIVE.name)
            .set(UserTable.DELETED_AT, null as LocalDateTime?)
            .set(UserTable.LAST_LOGIN_AT, now)
            .set(UserTable.UPDATED_AT, now)
            .returningResult(USER_FIELDS)
            .fetchOne(::toUser)
            ?: error("Failed to upsert OAuth user")
    }

    fun update(
        id: Long,
        nickname: String?,
        profileImageId: Long?,
    ) {
        val now = LocalDateTime.now()
        var step = dsl.update(UserTable.USERS).set(UserTable.UPDATED_AT, now)
        nickname?.let { step = step.set(UserTable.NICKNAME, it) }
        profileImageId?.let { step = step.set(UserTable.PROFILE_IMAGE_ID, it) }
        step.where(UserTable.ID.eq(id)).execute()
    }

    private fun toUser(record: Record): User =
        User(
            id = record.get(UserTable.ID),
            provider = Provider.valueOf(record.get(UserTable.PROVIDER)),
            providerId = record.get(UserTable.PROVIDER_ID),
            email = record.get(UserTable.EMAIL),
            nickname = record.get(UserTable.NICKNAME),
            profileImageId = record.get(UserTable.PROFILE_IMAGE_ID),
            status = UserStatus.valueOf(record.get(UserTable.STATUS)),
            lastLoginAt = record.get(UserTable.LAST_LOGIN_AT),
            deletedAt = record.get(UserTable.DELETED_AT),
            createdAt = record.get(UserTable.CREATED_AT),
            updatedAt = record.get(UserTable.UPDATED_AT),
        )

    private companion object {
        val PROVIDER_CONFLICT_TARGET = DSL.field(DSL.name("provider"), String::class.java)
        val PROVIDER_ID_CONFLICT_TARGET = DSL.field(DSL.name("provider_id"), String::class.java)

        val USER_FIELDS: List<Field<*>> =
            listOf(
                UserTable.ID,
                UserTable.PROVIDER,
                UserTable.PROVIDER_ID,
                UserTable.EMAIL,
                UserTable.NICKNAME,
                UserTable.PROFILE_IMAGE_ID,
                UserTable.STATUS,
                UserTable.LAST_LOGIN_AT,
                UserTable.DELETED_AT,
                UserTable.CREATED_AT,
                UserTable.UPDATED_AT,
            )
    }
}
