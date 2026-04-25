package com.firstpenguin.app.domain.auth.repository

import com.firstpenguin.app.domain.auth.model.RefreshToken
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class RefreshTokenRepository(
    private val dsl: DSLContext,
) {
    fun save(
        userId: Long,
        deviceId: String,
        tokenHash: String,
        expiresAt: LocalDateTime,
    ) {
        val now = LocalDateTime.now()

        dsl
            .insertInto(RefreshTokenTable.REFRESH_TOKENS)
            .set(RefreshTokenTable.USER_ID, userId)
            .set(RefreshTokenTable.DEVICE_ID, deviceId)
            .set(RefreshTokenTable.TOKEN_HASH, tokenHash)
            .set(RefreshTokenTable.EXPIRES_AT, expiresAt)
            .set(RefreshTokenTable.CREATED_AT, now)
            .set(RefreshTokenTable.UPDATED_AT, now)
            .execute()
    }

    fun findByTokenHash(tokenHash: String): RefreshToken? =
        dsl
            .select(REFRESH_TOKEN_FIELDS)
            .from(RefreshTokenTable.REFRESH_TOKENS)
            .where(RefreshTokenTable.TOKEN_HASH.eq(tokenHash))
            .fetchOne(::toRefreshToken)

    fun updateToken(
        id: Long,
        tokenHash: String,
        expiresAt: LocalDateTime,
    ): Int =
        dsl
            .update(RefreshTokenTable.REFRESH_TOKENS)
            .set(RefreshTokenTable.TOKEN_HASH, tokenHash)
            .set(RefreshTokenTable.EXPIRES_AT, expiresAt)
            .set(RefreshTokenTable.UPDATED_AT, LocalDateTime.now())
            .where(RefreshTokenTable.ID.eq(id))
            .execute()

    fun deleteByTokenHash(tokenHash: String): Int =
        dsl
            .deleteFrom(RefreshTokenTable.REFRESH_TOKENS)
            .where(RefreshTokenTable.TOKEN_HASH.eq(tokenHash))
            .execute()

    fun deleteByUserId(userId: Long): Int =
        dsl
            .deleteFrom(RefreshTokenTable.REFRESH_TOKENS)
            .where(RefreshTokenTable.USER_ID.eq(userId))
            .execute()

    fun deleteById(id: Long): Int =
        dsl
            .deleteFrom(RefreshTokenTable.REFRESH_TOKENS)
            .where(RefreshTokenTable.ID.eq(id))
            .execute()

    private fun toRefreshToken(record: Record): RefreshToken =
        RefreshToken(
            id = record.get(RefreshTokenTable.ID),
            userId = record.get(RefreshTokenTable.USER_ID),
            deviceId = record.get(RefreshTokenTable.DEVICE_ID),
            tokenHash = record.get(RefreshTokenTable.TOKEN_HASH),
            expiresAt = record.get(RefreshTokenTable.EXPIRES_AT),
            createdAt = record.get(RefreshTokenTable.CREATED_AT),
            updatedAt = record.get(RefreshTokenTable.UPDATED_AT),
        )

    private companion object {
        val REFRESH_TOKEN_FIELDS: List<Field<*>> =
            listOf(
                RefreshTokenTable.ID,
                RefreshTokenTable.USER_ID,
                RefreshTokenTable.DEVICE_ID,
                RefreshTokenTable.TOKEN_HASH,
                RefreshTokenTable.EXPIRES_AT,
                RefreshTokenTable.CREATED_AT,
                RefreshTokenTable.UPDATED_AT,
            )
    }
}
