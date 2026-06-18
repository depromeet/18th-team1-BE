package com.firstpenguin.app.domain.user.repository

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
    fun `사용자 생성은 conflict 발생 시 update하지 않는다`() {
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

        UserRepository(dsl).create(USER_NICKNAME, LocalDateTime.now())

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

    private fun userResult(dsl: DSLContext) =
        dsl.newResult(*USER_FIELDS).apply {
            add(userRecord(dsl))
        }

    private fun userRecord(dsl: DSLContext): Record {
        val now = LocalDateTime.now()

        return dsl.newRecord(*USER_FIELDS).apply {
            set(UserTable.ID, USER_ID)
            set(UserTable.NICKNAME, USER_NICKNAME)
            set(UserTable.PROFILE_IMAGE_ID, null as Long?)
            set(UserTable.STATUS, UserStatus.ACTIVE.name)
            set(UserTable.DELETED_AT, null as LocalDateTime?)
            set(UserTable.CREATED_AT, now)
            set(UserTable.UPDATED_AT, now)
        }
    }

    private companion object {
        const val USER_ID = 1L
        const val USER_NICKNAME = "랜덤펭귄"
        val USER_FIELDS: Array<Field<*>> =
            arrayOf(
                UserTable.ID,
                UserTable.NICKNAME,
                UserTable.PROFILE_IMAGE_ID,
                UserTable.STATUS,
                UserTable.DELETED_AT,
                UserTable.CREATED_AT,
                UserTable.UPDATED_AT,
            )
    }
}
