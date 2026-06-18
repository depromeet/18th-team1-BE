package com.firstpenguin.app.domain.user.service

import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.model.UserStatus
import com.firstpenguin.app.domain.user.repository.UserRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.dao.DuplicateKeyException
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

    @Test
    fun `프로필 수정 시 닉네임이 이미 존재하면 실패한다`() {
        Mockito.`when`(userRepository.existsByNickname(DUPLICATE_NICKNAME, USER_ID)).thenReturn(true)

        val exception =
            assertFailsWith<CustomException> {
                userService.updateProfile(USER_ID, DUPLICATE_NICKNAME, null)
            }

        assertEquals(ErrorCode.NICKNAME_ALREADY_EXISTS, exception.errorCode)
        Mockito.verify(userRepository, Mockito.never()).update(USER_ID, DUPLICATE_NICKNAME, null)
    }

    @Test
    fun `프로필 수정 시 예약 닉네임이면 실패한다`() {
        val exception =
            assertFailsWith<CustomException> {
                userService.updateProfile(USER_ID, DEV_USER_NICKNAME, null)
            }

        assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
        Mockito.verify(userRepository, Mockito.never()).existsByNickname(DEV_USER_NICKNAME, USER_ID)
        Mockito.verify(userRepository, Mockito.never()).update(USER_ID, DEV_USER_NICKNAME, null)
    }

    @Test
    fun `프로필 수정 중 닉네임 unique 충돌이 발생하면 닉네임 중복 예외로 변환한다`() {
        Mockito
            .doThrow(nicknameDuplicateException())
            .`when`(userRepository)
            .update(USER_ID, DUPLICATE_NICKNAME, null)

        val exception =
            assertFailsWith<CustomException> {
                userService.updateProfile(USER_ID, DUPLICATE_NICKNAME, null)
            }

        assertEquals(ErrorCode.NICKNAME_ALREADY_EXISTS, exception.errorCode)
    }

    @Test
    fun `닉네임 수정 요청이 아니면 중복 예외를 변환하지 않는다`() {
        Mockito
            .doThrow(DuplicateKeyException("unexpected_unique_idx"))
            .`when`(userRepository)
            .update(USER_ID, null, PROFILE_IMAGE_ID)

        assertFailsWith<DuplicateKeyException> {
            userService.updateProfile(USER_ID, null, PROFILE_IMAGE_ID)
        }
    }

    private fun nicknameDuplicateException(): DuplicateKeyException =
        DuplicateKeyException("duplicate key value violates unique constraint \"$USER_NICKNAME_UNIQUE_INDEX_NAME\"")

    private fun user(
        status: UserStatus = UserStatus.ACTIVE,
        deletedAt: LocalDateTime? = null,
    ): User {
        val now = LocalDateTime.now()

        return User(
            id = USER_ID,
            nickname = "penguin",
            profileImageId = null,
            status = status,
            deletedAt = deletedAt,
            createdAt = now,
            updatedAt = now,
        )
    }

    private companion object {
        const val USER_ID = 1L
        const val PROFILE_IMAGE_ID = 10L
        const val DEV_USER_NICKNAME = "개발자"
        const val DUPLICATE_NICKNAME = "조용한토끼"
        const val USER_NICKNAME_UNIQUE_INDEX_NAME = "users_nickname_unique_idx"
    }
}
