package com.firstpenguin.app.domain.user.usecase

import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.Provider
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.model.UserStatus
import com.firstpenguin.app.domain.user.service.UserNicknameGenerator
import com.firstpenguin.app.domain.user.service.UserService
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.dao.DuplicateKeyException
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OAuthUserUseCaseTest {
    private lateinit var userNicknameGenerator: UserNicknameGenerator
    private lateinit var userService: UserService
    private lateinit var oAuthUserUseCase: OAuthUserUseCase

    @BeforeEach
    fun setUp() {
        userNicknameGenerator = Mockito.mock(UserNicknameGenerator::class.java)
        userService = Mockito.mock(UserService::class.java)
        oAuthUserUseCase = OAuthUserUseCase(userNicknameGenerator, userService)
    }

    @Test
    fun `닉네임 중복이면 닉네임을 다시 생성해 OAuth 사용자를 upsert 한다`() {
        val user = user(nickname = SECOND_NICKNAME)
        Mockito.`when`(userNicknameGenerator.generate()).thenReturn(FIRST_NICKNAME, SECOND_NICKNAME)
        Mockito
            .`when`(userService.upsertOAuthUser(OAUTH_USER_PROFILE, FIRST_NICKNAME))
            .thenThrow(nicknameDuplicateException())
        Mockito.`when`(userService.upsertOAuthUser(OAUTH_USER_PROFILE, SECOND_NICKNAME)).thenReturn(user)

        val result = oAuthUserUseCase.upsertOAuthUser(OAUTH_USER_PROFILE)

        assertEquals(user, result)
        Mockito.verify(userService).upsertOAuthUser(OAUTH_USER_PROFILE, FIRST_NICKNAME)
        Mockito.verify(userService).upsertOAuthUser(OAUTH_USER_PROFILE, SECOND_NICKNAME)
    }

    @Test
    fun `닉네임 중복이 아니면 재시도하지 않는다`() {
        Mockito.`when`(userNicknameGenerator.generate()).thenReturn(FIRST_NICKNAME)
        Mockito
            .`when`(userService.upsertOAuthUser(OAUTH_USER_PROFILE, FIRST_NICKNAME))
            .thenThrow(DuplicateKeyException("users_provider_provider_id_unique"))

        assertFailsWith<DuplicateKeyException> {
            oAuthUserUseCase.upsertOAuthUser(OAUTH_USER_PROFILE)
        }

        Mockito.verify(userService).upsertOAuthUser(OAUTH_USER_PROFILE, FIRST_NICKNAME)
        Mockito.verifyNoMoreInteractions(userService)
    }

    @Test
    fun `닉네임 생성 재시도 횟수를 초과하면 실패한다`() {
        Mockito.`when`(userNicknameGenerator.generate()).thenReturn(FIRST_NICKNAME)
        Mockito
            .`when`(userService.upsertOAuthUser(OAUTH_USER_PROFILE, FIRST_NICKNAME))
            .thenThrow(nicknameDuplicateException())

        val exception =
            assertFailsWith<CustomException> {
                oAuthUserUseCase.upsertOAuthUser(OAUTH_USER_PROFILE)
            }

        assertEquals(ErrorCode.NICKNAME_GENERATION_FAILED, exception.errorCode)
        Mockito.verify(userService, Mockito.times(10)).upsertOAuthUser(OAUTH_USER_PROFILE, FIRST_NICKNAME)
    }

    private fun nicknameDuplicateException(): DuplicateKeyException =
        DuplicateKeyException("duplicate key value violates unique constraint \"$USER_NICKNAME_UNIQUE_INDEX_NAME\"")

    private fun user(nickname: String): User {
        val now = LocalDateTime.now()

        return User(
            id = USER_ID,
            provider = Provider.KAKAO,
            providerId = OAUTH_USER_PROFILE.providerId,
            email = OAUTH_USER_PROFILE.email,
            providerDisplayName = OAUTH_USER_PROFILE.providerDisplayName,
            nickname = nickname,
            profileImageId = null,
            status = UserStatus.ACTIVE,
            lastLoginAt = now,
            deletedAt = null,
            createdAt = now,
            updatedAt = now,
        )
    }

    private companion object {
        const val USER_ID = 1L
        const val FIRST_NICKNAME = "조용한토끼"
        const val SECOND_NICKNAME = "책읽는펭귄"
        const val USER_NICKNAME_UNIQUE_INDEX_NAME = "users_nickname_unique_idx"
        val OAUTH_USER_PROFILE =
            OAuthUserProfile(
                provider = Provider.KAKAO,
                providerId = "oauth-user",
                email = "oauth@example.com",
                providerDisplayName = "OAuth User",
            )
    }
}
