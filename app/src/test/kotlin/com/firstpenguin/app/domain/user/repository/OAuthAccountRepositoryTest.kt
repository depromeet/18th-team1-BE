package com.firstpenguin.app.domain.user.repository

import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.Provider
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OAuthAccountRepositoryTest {
    @Test
    fun `활성 OAuth 계정 조회는 연결 해제되지 않은 계정만 조회한다`() {
        val capturedSql =
            captureSql { dsl ->
                OAuthAccountRepository(dsl).findActiveByProviderAndProviderId(Provider.KAKAO, PROVIDER_ID)
            }

        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")
        assertTrue(normalizedSql.contains(""""oauth_accounts"."disconnected_at" is null"""), normalizedSql)
    }

    @Test
    fun `OAuth 계정 생성은 conflict 발생 시 update하지 않는다`() {
        val capturedSql =
            captureSql { dsl ->
                OAuthAccountRepository(dsl).create(USER_ID, OAUTH_USER_PROFILE, LocalDateTime.now())
            }

        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")
        assertTrue(normalizedSql.contains("""on conflict do nothing"""), normalizedSql)
        assertFalse(normalizedSql.contains("""do update"""), normalizedSql)
    }

    @Test
    fun `OAuth 로그인 정보 갱신은 이메일이 null이면 기존 이메일을 유지한다`() {
        val capturedSql =
            captureSql { dsl ->
                OAuthAccountRepository(dsl).updateLogin(
                    OAUTH_ACCOUNT_ID,
                    OAUTH_USER_PROFILE.copy(email = null),
                    LocalDateTime.now(),
                )
            }

        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")
        assertFalse(normalizedSql.contains("\"oauth_accounts\".\"email\" ="), normalizedSql)
    }

    private fun captureSql(repositoryCall: (DSLContext) -> Unit): String {
        var capturedSql = ""
        lateinit var dsl: DSLContext
        val connection =
            MockConnection(
                MockDataProvider { context ->
                    capturedSql = context.sql()
                    arrayOf(MockResult(1, oAuthAccountResult(dsl)))
                },
            )
        dsl = DSL.using(connection, SQLDialect.POSTGRES)
        repositoryCall(dsl)
        return capturedSql
    }

    private fun oAuthAccountResult(dsl: DSLContext) =
        dsl.newResult(*OAUTH_ACCOUNT_FIELDS).apply {
            add(oAuthAccountRecord(dsl))
        }

    private fun oAuthAccountRecord(dsl: DSLContext): Record {
        val now = LocalDateTime.now()

        return dsl.newRecord(*OAUTH_ACCOUNT_FIELDS).apply {
            set(OAuthAccountTable.ID, OAUTH_ACCOUNT_ID)
            set(OAuthAccountTable.USER_ID, USER_ID)
            set(OAuthAccountTable.PROVIDER, Provider.KAKAO.name)
            set(OAuthAccountTable.PROVIDER_ID, PROVIDER_ID)
            set(OAuthAccountTable.EMAIL, OAUTH_USER_PROFILE.email)
            set(OAuthAccountTable.PROVIDER_DISPLAY_NAME, OAUTH_USER_PROFILE.providerDisplayName)
            set(OAuthAccountTable.LAST_LOGIN_AT, now)
            set(OAuthAccountTable.DISCONNECTED_AT, null as LocalDateTime?)
            set(OAuthAccountTable.CREATED_AT, now)
            set(OAuthAccountTable.UPDATED_AT, now)
        }
    }

    private companion object {
        const val OAUTH_ACCOUNT_ID = 10L
        const val USER_ID = 1L
        const val PROVIDER_ID = "provider-id"
        val OAUTH_USER_PROFILE =
            OAuthUserProfile(
                provider = Provider.KAKAO,
                providerId = PROVIDER_ID,
                email = "user@example.com",
                providerDisplayName = "provider user",
            )
        val OAUTH_ACCOUNT_FIELDS: Array<Field<*>> =
            arrayOf(
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
