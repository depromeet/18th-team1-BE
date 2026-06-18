package com.firstpenguin.app.domain.user.repository

import com.firstpenguin.app.domain.user.model.OAuthAccount
import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.Provider
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.UpdateSetMoreStep
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class OAuthAccountRepository(
    private val dsl: DSLContext,
) {
    fun findActiveByProviderAndProviderId(
        provider: Provider,
        providerId: String,
    ): OAuthAccount? =
        dsl
            .select(OAUTH_ACCOUNT_FIELDS)
            .from(OAuthAccountTable.OAUTH_ACCOUNTS)
            .where(OAuthAccountTable.PROVIDER.eq(provider.name))
            .and(OAuthAccountTable.PROVIDER_ID.eq(providerId))
            .and(OAuthAccountTable.DISCONNECTED_AT.isNull)
            .fetchOne(::toOAuthAccount)

    fun findActiveByUserId(userId: Long): OAuthAccount? =
        dsl
            .select(OAUTH_ACCOUNT_FIELDS)
            .from(OAuthAccountTable.OAUTH_ACCOUNTS)
            .where(OAuthAccountTable.USER_ID.eq(userId))
            .and(OAuthAccountTable.DISCONNECTED_AT.isNull)
            .fetchOne(::toOAuthAccount)

    fun create(
        userId: Long,
        profile: OAuthUserProfile,
        now: LocalDateTime,
    ): OAuthAccount? =
        dsl
            .insertInto(OAuthAccountTable.OAUTH_ACCOUNTS)
            .set(OAuthAccountTable.USER_ID, userId)
            .set(OAuthAccountTable.PROVIDER, profile.provider.name)
            .set(OAuthAccountTable.PROVIDER_ID, profile.providerId)
            .set(OAuthAccountTable.EMAIL, profile.email)
            .set(OAuthAccountTable.PROVIDER_DISPLAY_NAME, profile.providerDisplayName)
            .set(OAuthAccountTable.LAST_LOGIN_AT, now)
            .set(OAuthAccountTable.CREATED_AT, now)
            .set(OAuthAccountTable.UPDATED_AT, now)
            .onConflictDoNothing()
            .returningResult(OAUTH_ACCOUNT_FIELDS)
            .fetchOne(::toOAuthAccount)

    fun updateLogin(
        id: Long,
        profile: OAuthUserProfile,
        now: LocalDateTime,
    ): OAuthAccount? =
        oauthLoginUpdateStep(profile, now)
            .where(OAuthAccountTable.ID.eq(id))
            .and(OAuthAccountTable.DISCONNECTED_AT.isNull)
            .returningResult(OAUTH_ACCOUNT_FIELDS)
            .fetchOne(::toOAuthAccount)

    private fun oauthLoginUpdateStep(
        profile: OAuthUserProfile,
        now: LocalDateTime,
    ): UpdateSetMoreStep<Record> {
        var step =
            dsl
                .update(OAuthAccountTable.OAUTH_ACCOUNTS)
                .set(OAuthAccountTable.PROVIDER_DISPLAY_NAME, profile.providerDisplayName)
                .set(OAuthAccountTable.LAST_LOGIN_AT, now)
                .set(OAuthAccountTable.UPDATED_AT, now)

        profile.email?.let { step = step.set(OAuthAccountTable.EMAIL, it) }
        return step
    }

    private fun toOAuthAccount(record: Record): OAuthAccount =
        OAuthAccount(
            id = record.get(OAuthAccountTable.ID),
            userId = record.get(OAuthAccountTable.USER_ID),
            provider = Provider.valueOf(record.get(OAuthAccountTable.PROVIDER)),
            providerId = record.get(OAuthAccountTable.PROVIDER_ID),
            email = record.get(OAuthAccountTable.EMAIL),
            providerDisplayName = record.get(OAuthAccountTable.PROVIDER_DISPLAY_NAME),
            lastLoginAt = record.get(OAuthAccountTable.LAST_LOGIN_AT),
            disconnectedAt = record.get(OAuthAccountTable.DISCONNECTED_AT),
            createdAt = record.get(OAuthAccountTable.CREATED_AT),
            updatedAt = record.get(OAuthAccountTable.UPDATED_AT),
        )

    private companion object {
        val OAUTH_ACCOUNT_FIELDS: List<Field<*>> =
            listOf(
                OAuthAccountTable.ID,
                OAuthAccountTable.USER_ID,
                OAuthAccountTable.PROVIDER,
                OAuthAccountTable.PROVIDER_ID,
                OAuthAccountTable.EMAIL,
                OAuthAccountTable.PROVIDER_DISPLAY_NAME,
                OAuthAccountTable.LAST_LOGIN_AT,
                OAuthAccountTable.DISCONNECTED_AT,
                OAuthAccountTable.CREATED_AT,
                OAuthAccountTable.UPDATED_AT,
            )
    }
}
