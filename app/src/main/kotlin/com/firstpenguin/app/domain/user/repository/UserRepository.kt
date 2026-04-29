package com.firstpenguin.app.domain.user.repository

import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.Provider
import com.firstpenguin.app.domain.user.model.Role
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.model.UserStatus
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
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

    fun findByProviderId(providerId: String): User? =
        dsl
            .select(USER_FIELDS)
            .from(UserTable.USERS)
            .where(UserTable.PROVIDER_ID.eq(providerId))
            .fetchOne(::toUser)

    @Transactional
    fun upsertOAuthUser(profile: OAuthUserProfile): User {
        val existingUser = findByProviderId(profile.providerId)

        if (existingUser == null) {
            return insertOAuthUser(profile)
        }

        return updateOAuthUser(existingUser, profile)
    }

    private fun insertOAuthUser(profile: OAuthUserProfile): User {
        val now = LocalDateTime.now()

        return dsl
            .insertInto(UserTable.USERS)
            .set(UserTable.PROVIDER, profile.provider.name)
            .set(UserTable.PROVIDER_ID, profile.providerId)
            .set(UserTable.EMAIL, profile.email)
            .set(UserTable.NICKNAME, profile.nickname)
            .set(UserTable.PROFILE_IMAGE_KEY, profile.profileImageKey)
            .set(UserTable.STATUS, UserStatus.ACTIVE.name)
            .set(UserTable.ROLE, Role.USER.name)
            .set(UserTable.LAST_LOGIN_AT, now)
            .set(UserTable.CREATED_AT, now)
            .set(UserTable.UPDATED_AT, now)
            .returningResult(USER_FIELDS)
            .fetchOne(::toUser)
            ?: error("Failed to insert OAuth user")
    }

    private fun updateOAuthUser(
        user: User,
        profile: OAuthUserProfile,
    ): User {
        val now = LocalDateTime.now()

        return dsl
            .update(UserTable.USERS)
            .set(UserTable.EMAIL, profile.email ?: user.email)
            .set(UserTable.NICKNAME, profile.nickname)
            .set(UserTable.PROFILE_IMAGE_KEY, profile.profileImageKey ?: user.profileImageKey)
            .set(UserTable.STATUS, UserStatus.ACTIVE.name)
            .set(UserTable.DELETED_AT, null as LocalDateTime?)
            .set(UserTable.LAST_LOGIN_AT, now)
            .set(UserTable.UPDATED_AT, now)
            .where(UserTable.ID.eq(user.id))
            .returningResult(USER_FIELDS)
            .fetchOne(::toUser)
            ?: error("Failed to update OAuth user")
    }

    private fun toUser(record: Record): User =
        User(
            id = record.get(UserTable.ID),
            provider = Provider.valueOf(record.get(UserTable.PROVIDER)),
            providerId = record.get(UserTable.PROVIDER_ID),
            email = record.get(UserTable.EMAIL),
            nickname = record.get(UserTable.NICKNAME),
            profileImageKey = record.get(UserTable.PROFILE_IMAGE_KEY),
            status = UserStatus.valueOf(record.get(UserTable.STATUS)),
            role = Role.valueOf(record.get(UserTable.ROLE)),
            lastLoginAt = record.get(UserTable.LAST_LOGIN_AT),
            deletedAt = record.get(UserTable.DELETED_AT),
            createdAt = record.get(UserTable.CREATED_AT),
            updatedAt = record.get(UserTable.UPDATED_AT),
        )

    private companion object {
        val USER_FIELDS: List<Field<*>> =
            listOf(
                UserTable.ID,
                UserTable.PROVIDER,
                UserTable.PROVIDER_ID,
                UserTable.EMAIL,
                UserTable.NICKNAME,
                UserTable.PROFILE_IMAGE_KEY,
                UserTable.STATUS,
                UserTable.ROLE,
                UserTable.LAST_LOGIN_AT,
                UserTable.DELETED_AT,
                UserTable.CREATED_AT,
                UserTable.UPDATED_AT,
            )
    }
}
