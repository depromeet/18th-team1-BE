package com.firstpenguin.app.domain.user.repository

import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.Provider
import com.firstpenguin.app.domain.user.model.UserStatus
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

class UserRepositoryTest {
    @Test
    fun `OAuth 사용자 생성은 conflict 발생 시 update하지 않는다`() {
        var capturedSql = ""
        lateinit var dsl: DSLContext
        val connection =
            MockConnection(
                MockDataProvider { context ->
                    capturedSql = context.sql()
                    arrayOf(MockResult(1, userResult(dsl)))
                },
            )
        dsl = DSL.using(connection, SQLDialect.POSTGRES)

        UserRepository(dsl).createOAuthUser(OAUTH_USER_PROFILE, OAUTH_USER_NICKNAME)

        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")
        assertTrue(
            normalizedSql.contains("""on conflict do nothing"""),
            normalizedSql,
        )
        assertFalse(
            normalizedSql.contains("""do update"""),
            normalizedSql,
        )
    }

    @Test
    fun `OAuth 로그인 정보 갱신은 닉네임을 변경하지 않는다`() {
        val capturedSql =
            captureSql { dsl ->
                UserRepository(dsl).updateOAuthLogin(OAUTH_USER_PROFILE)
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertTrue(normalizedSql.startsWith("""update "users" set"""), normalizedSql)
        assertFalse(normalizedSql.contains("\"users\".\"nickname\" ="), normalizedSql)
    }

    @Test
    fun `OAuth 로그인 정보 갱신은 이메일이 null이면 기존 이메일을 유지한다`() {
        val capturedSql =
            captureSql { dsl ->
                UserRepository(dsl).updateOAuthLogin(OAUTH_USER_PROFILE.copy(email = null))
            }
        val normalizedSql = capturedSql.replace(Regex("\\s+"), " ")

        assertFalse(normalizedSql.contains("\"users\".\"email\" ="), normalizedSql)
    }

    private fun captureSql(repositoryCall: (DSLContext) -> Unit): String {
        var capturedSql = ""
        lateinit var dsl: DSLContext
        val connection =
            MockConnection(
                MockDataProvider { context ->
                    capturedSql = context.sql()
                    arrayOf(MockResult(1, userResult(dsl)))
                },
            )
        dsl = DSL.using(connection, SQLDialect.POSTGRES)
        repositoryCall(dsl)
        return capturedSql
    }

    private fun userResult(dsl: DSLContext) =
        dsl.newResult(*USER_FIELDS).apply {
            add(userRecord(dsl))
        }

    private fun userRecord(dsl: DSLContext): Record {
        val now = LocalDateTime.now()

        return dsl.newRecord(*USER_FIELDS).apply {
            set(UserTable.ID, USER_ID)
            set(UserTable.PROVIDER, Provider.KAKAO.name)
            set(UserTable.PROVIDER_ID, "dev-user")
            set(UserTable.EMAIL, "dev@firstpenguin.com")
            set(UserTable.PROVIDER_DISPLAY_NAME, "개발자")
            set(UserTable.NICKNAME, "개발자")
            set(UserTable.PROFILE_IMAGE_ID, null as Long?)
            set(UserTable.STATUS, UserStatus.ACTIVE.name)
            set(UserTable.LAST_LOGIN_AT, now)
            set(UserTable.DELETED_AT, null as LocalDateTime?)
            set(UserTable.CREATED_AT, now)
            set(UserTable.UPDATED_AT, now)
        }
    }

    private companion object {
        const val USER_ID = 1L
        const val OAUTH_USER_NICKNAME = "랜덤펭귄"
        val OAUTH_USER_PROFILE =
            OAuthUserProfile(
                provider = Provider.KAKAO,
                providerId = "dev-user",
                email = "dev@firstpenguin.com",
                providerDisplayName = "개발자",
            )
        val USER_FIELDS: Array<Field<*>> =
            arrayOf(
                UserTable.ID,
                UserTable.PROVIDER,
                UserTable.PROVIDER_ID,
                UserTable.EMAIL,
                UserTable.PROVIDER_DISPLAY_NAME,
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
