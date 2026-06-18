package com.firstpenguin.app.domain.user.service

import com.firstpenguin.app.domain.user.model.OAuthAccount
import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.Provider
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.model.UserStatus
import com.firstpenguin.app.domain.user.repository.OAuthAccountRepository
import com.firstpenguin.app.domain.user.repository.UserRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OAuthUserServiceTest {
    private lateinit var oAuthAccountRepository: OAuthAccountRepository
    private lateinit var userRepository: UserRepository
    private lateinit var oAuthUserService: OAuthUserService

    @BeforeEach
    fun setUp() {
        oAuthAccountRepository = Mockito.mock(OAuthAccountRepository::class.java)
        userRepository = Mockito.mock(UserRepository::class.java)
        oAuthUserService = OAuthUserService(oAuthAccountRepository, userRepository)
    }

    @Test
    fun `OAuth 계정으로 사용자를 조회한다`() {
        val account = oAuthAccount()
        val user = user()
        Mockito
            .`when`(oAuthAccountRepository.findActiveByProviderAndProviderId(Provider.KAKAO, PROVIDER_ID))
            .thenReturn(account)
        Mockito.`when`(userRepository.findById(USER_ID)).thenReturn(user)

        val result = oAuthUserService.findOAuthUser(OAUTH_USER_PROFILE)

        assertEquals(user, result)
    }

    @Test
    fun `OAuth 사용자 생성 시 user와 OAuth 계정을 함께 생성한다`() {
        val user = user()
        val account = oAuthAccount()
        Mockito.`when`(userRepository.create(eqValue(USER_NICKNAME), anyDateTime())).thenReturn(user)
        Mockito
            .`when`(oAuthAccountRepository.create(eqValue(USER_ID), eqValue(OAUTH_USER_PROFILE), anyDateTime()))
            .thenReturn(account)

        val result = oAuthUserService.createOAuthUser(OAUTH_USER_PROFILE, USER_NICKNAME)

        assertEquals(user, result)
    }

    @Test
    fun `활성 OAuth 사용자는 OAuth 계정 로그인 정보를 갱신한다`() {
        val user = user()
        val account = oAuthAccount()
        Mockito
            .`when`(oAuthAccountRepository.findActiveByProviderAndProviderId(Provider.KAKAO, PROVIDER_ID))
            .thenReturn(account)
        Mockito
            .`when`(
                oAuthAccountRepository.updateLogin(
                    eqValue(OAUTH_ACCOUNT_ID),
                    eqValue(OAUTH_USER_PROFILE),
                    anyDateTime(),
                ),
            ).thenReturn(account)

        val result = oAuthUserService.updateOAuthLogin(user, OAUTH_USER_PROFILE)

        assertEquals(user, result)
        Mockito.verify(oAuthAccountRepository).updateLogin(
            eqValue(OAUTH_ACCOUNT_ID),
            eqValue(OAUTH_USER_PROFILE),
            anyDateTime(),
        )
    }

    @Test
    fun `OAuth 계정의 userId가 다르면 로그인 정보를 갱신하지 않는다`() {
        val account = oAuthAccount(userId = OTHER_USER_ID)
        Mockito
            .`when`(oAuthAccountRepository.findActiveByProviderAndProviderId(Provider.KAKAO, PROVIDER_ID))
            .thenReturn(account)

        val exception =
            assertFailsWith<CustomException> {
                oAuthUserService.updateOAuthLogin(user(), OAUTH_USER_PROFILE)
            }

        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, exception.errorCode)
        Mockito.verify(oAuthAccountRepository, Mockito.never()).updateLogin(
            eqValue(OAUTH_ACCOUNT_ID),
            eqValue(OAUTH_USER_PROFILE),
            anyDateTime(),
        )
    }

    @Test
    fun `차단 OAuth 사용자는 로그인 정보를 갱신하지 않는다`() {
        Mockito
            .`when`(oAuthAccountRepository.findActiveByProviderAndProviderId(Provider.KAKAO, PROVIDER_ID))
            .thenReturn(oAuthAccount())

        val exception =
            assertFailsWith<CustomException> {
                oAuthUserService.updateOAuthLogin(user(status = UserStatus.BLOCKED), OAUTH_USER_PROFILE)
            }

        assertEquals(ErrorCode.AUTH_USER_BLOCKED, exception.errorCode)
        Mockito.verify(oAuthAccountRepository, Mockito.never()).updateLogin(
            eqValue(OAUTH_ACCOUNT_ID),
            eqValue(OAUTH_USER_PROFILE),
            anyDateTime(),
        )
    }

    private fun anyDateTime(): LocalDateTime {
        Mockito.any(LocalDateTime::class.java)
        return LocalDateTime.MIN
    }

    private fun <T> eqValue(value: T): T {
        Mockito.eq(value)
        return value
    }

    private fun user(
        status: UserStatus = UserStatus.ACTIVE,
        deletedAt: LocalDateTime? = null,
    ): User {
        val now = LocalDateTime.now()

        return User(
            id = USER_ID,
            nickname = USER_NICKNAME,
            profileImageId = null,
            status = status,
            deletedAt = deletedAt,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun oAuthAccount(userId: Long = USER_ID): OAuthAccount {
        val now = LocalDateTime.now()

        return OAuthAccount(
            id = OAUTH_ACCOUNT_ID,
            userId = userId,
            provider = Provider.KAKAO,
            providerId = PROVIDER_ID,
            email = OAUTH_USER_PROFILE.email,
            providerDisplayName = OAUTH_USER_PROFILE.providerDisplayName,
            lastLoginAt = now,
            disconnectedAt = null,
            createdAt = now,
            updatedAt = now,
        )
    }

    private companion object {
        const val OAUTH_ACCOUNT_ID = 10L
        const val USER_ID = 1L
        const val OTHER_USER_ID = 2L
        const val PROVIDER_ID = "provider-id"
        const val USER_NICKNAME = "조용한토끼"
        val OAUTH_USER_PROFILE =
            OAuthUserProfile(
                provider = Provider.KAKAO,
                providerId = PROVIDER_ID,
                email = "user@example.com",
                providerDisplayName = "provider user",
            )
    }
}
