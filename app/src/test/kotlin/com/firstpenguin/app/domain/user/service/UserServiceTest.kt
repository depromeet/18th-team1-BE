package com.firstpenguin.app.domain.user.service

import com.firstpenguin.app.domain.user.model.Provider
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.model.UserStatus
import com.firstpenguin.app.domain.user.repository.UserRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userRepository = Mockito.mock(UserRepository::class.java)
        userService = UserService(userRepository)
    }

    @Test
    fun `인증 사용자가 존재하지 않으면 인증 실패`() {
        Mockito.`when`(userRepository.findById(USER_ID)).thenReturn(null)

        val exception = assertFailsWith<CustomException> { userService.validateAuthenticatableUser(USER_ID) }

        assertEquals(ErrorCode.AUTH_USER_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `탈퇴 상태 사용자는 인증 실패`() {
        Mockito.`when`(userRepository.findById(USER_ID)).thenReturn(user(status = UserStatus.DELETED))

        val exception = assertFailsWith<CustomException> { userService.validateAuthenticatableUser(USER_ID) }

        assertEquals(ErrorCode.AUTH_USER_DELETED, exception.errorCode)
    }

    @Test
    fun `삭제 시간이 있으면 인증 실패`() {
        val deletedAt = LocalDateTime.now()
        Mockito.`when`(userRepository.findById(USER_ID)).thenReturn(user(deletedAt = deletedAt))

        val exception = assertFailsWith<CustomException> { userService.validateAuthenticatableUser(USER_ID) }

        assertEquals(ErrorCode.AUTH_USER_DELETED, exception.errorCode)
    }

    @Test
    fun `차단 상태 사용자는 인증 실패`() {
        Mockito.`when`(userRepository.findById(USER_ID)).thenReturn(user(status = UserStatus.BLOCKED))

        val exception = assertFailsWith<CustomException> { userService.validateAuthenticatableUser(USER_ID) }

        assertEquals(ErrorCode.AUTH_USER_BLOCKED, exception.errorCode)
    }

    @Test
    fun `활성 사용자는 인증 가능`() {
        Mockito.`when`(userRepository.findById(USER_ID)).thenReturn(user())

        userService.validateAuthenticatableUser(USER_ID)
    }

    private fun user(
        status: UserStatus = UserStatus.ACTIVE,
        deletedAt: LocalDateTime? = null,
    ): User {
        val now = LocalDateTime.now()

        return User(
            id = USER_ID,
            provider = Provider.KAKAO,
            providerId = "provider-id",
            email = "user@example.com",
            nickname = "penguin",
            profileImageId = null,
            status = status,
            lastLoginAt = now,
            deletedAt = deletedAt,
            createdAt = now,
            updatedAt = now,
        )
    }

    private companion object {
        const val USER_ID = 1L
    }
}
