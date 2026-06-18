package com.firstpenguin.app.domain.user.repository

import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.model.UserStatus
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
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

    fun create(
        nickname: String,
        now: LocalDateTime,
    ): User? =
        dsl
            .insertInto(UserTable.USERS)
            .set(UserTable.NICKNAME, nickname)
            .set(UserTable.STATUS, UserStatus.ACTIVE.name)
            .set(UserTable.CREATED_AT, now)
            .set(UserTable.UPDATED_AT, now)
            .onConflictDoNothing()
            .returningResult(USER_FIELDS)
            .fetchOne(::toUser)

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

    fun requestWithdrawal(
        id: Long,
        requestedAt: LocalDateTime,
        dueAt: LocalDateTime,
    ): Int =
        dsl
            .update(UserTable.USERS)
            .set(UserTable.STATUS, UserStatus.WITHDRAWAL_REQUESTED.name)
            .set(UserTable.WITHDRAWAL_REQUESTED_AT, requestedAt)
            .set(UserTable.WITHDRAWAL_DUE_AT, dueAt)
            .set(UserTable.UPDATED_AT, requestedAt)
            .where(UserTable.ID.eq(id))
            .and(UserTable.STATUS.eq(UserStatus.ACTIVE.name))
            .execute()

    fun reactivateWithdrawalRequested(
        id: Long,
        now: LocalDateTime,
    ): User? =
        dsl
            .update(UserTable.USERS)
            .set(UserTable.STATUS, UserStatus.ACTIVE.name)
            .set(UserTable.WITHDRAWAL_REQUESTED_AT, null as LocalDateTime?)
            .set(UserTable.WITHDRAWAL_DUE_AT, null as LocalDateTime?)
            .set(UserTable.UPDATED_AT, now)
            .where(UserTable.ID.eq(id))
            .and(UserTable.STATUS.eq(UserStatus.WITHDRAWAL_REQUESTED.name))
            .and(UserTable.WITHDRAWAL_DUE_AT.gt(now))
            .returningResult(USER_FIELDS)
            .fetchOne(::toUser)

    fun existsByNickname(
        nickname: String,
        excludedUserId: Long,
    ): Boolean =
        dsl.fetchExists(
            dsl
                .selectOne()
                .from(UserTable.USERS)
                .where(UserTable.NICKNAME.eq(nickname))
                .and(UserTable.ID.ne(excludedUserId))
                .and(UserTable.DELETED_AT.isNull),
        )

    private fun toUser(record: Record): User =
        User(
            id = record.get(UserTable.ID),
            nickname = record.get(UserTable.NICKNAME),
            profileImageId = record.get(UserTable.PROFILE_IMAGE_ID),
            status = UserStatus.valueOf(record.get(UserTable.STATUS)),
            withdrawalRequestedAt = record.get(UserTable.WITHDRAWAL_REQUESTED_AT),
            withdrawalDueAt = record.get(UserTable.WITHDRAWAL_DUE_AT),
            deletedAt = record.get(UserTable.DELETED_AT),
            createdAt = record.get(UserTable.CREATED_AT),
            updatedAt = record.get(UserTable.UPDATED_AT),
        )

    private companion object {
        val USER_FIELDS: List<Field<*>> =
            listOf(
                UserTable.ID,
                UserTable.NICKNAME,
                UserTable.PROFILE_IMAGE_ID,
                UserTable.STATUS,
                UserTable.WITHDRAWAL_REQUESTED_AT,
                UserTable.WITHDRAWAL_DUE_AT,
                UserTable.DELETED_AT,
                UserTable.CREATED_AT,
                UserTable.UPDATED_AT,
            )
    }
}
