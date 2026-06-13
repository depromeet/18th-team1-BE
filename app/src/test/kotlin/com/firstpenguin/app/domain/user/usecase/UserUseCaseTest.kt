package com.firstpenguin.app.domain.user.usecase

import com.firstpenguin.app.domain.image.service.ImageService
import com.firstpenguin.app.domain.user.model.Provider
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.model.UserStatus
import com.firstpenguin.app.domain.user.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime
import kotlin.test.assertEquals

class UserUseCaseTest {
    private lateinit var imageService: ImageService
    private lateinit var userService: UserService
    private lateinit var userUseCase: UserUseCase

    @BeforeEach
    fun setUp() {
        imageService = Mockito.mock(ImageService::class.java)
        userService = Mockito.mock(UserService::class.java)
        userUseCase = UserUseCase(imageService, userService)
    }

    @Test
    fun `내 정보 조회 응답에 OAuth provider와 가입한 날짜를 포함한다`() {
        val user = user()
        Mockito.`when`(userService.getById(USER_ID)).thenReturn(user)

        val response = userUseCase.getMe(USER_ID)

        assertEquals(user.provider.name, response.provider)
        assertEquals(user.createdAt.toLocalDate(), response.createdAt)
        Mockito.verifyNoInteractions(imageService)
    }

    private fun user(): User =
        User(
            id = USER_ID,
            provider = Provider.KAKAO,
            providerId = "provider-id",
            email = "user@example.com",
            providerDisplayName = "provider user",
            nickname = "penguin",
            profileImageId = null,
            status = UserStatus.ACTIVE,
            lastLoginAt = CREATED_AT,
            deletedAt = null,
            createdAt = CREATED_AT,
            updatedAt = CREATED_AT,
        )

    private companion object {
        const val USER_ID = 1L
        val CREATED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 13, 14, 30)
    }
}
