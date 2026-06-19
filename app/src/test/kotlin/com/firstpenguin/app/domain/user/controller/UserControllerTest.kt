package com.firstpenguin.app.domain.user.controller

import com.firstpenguin.app.domain.auth.token.RefreshTokenCookieManager
import com.firstpenguin.app.domain.user.dto.UserSignupDateResponse
import com.firstpenguin.app.domain.user.usecase.UserUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDate
import kotlin.test.assertEquals

class UserControllerTest {
    private lateinit var userUseCase: UserUseCase
    private lateinit var userController: UserController

    @BeforeEach
    fun setUp() {
        userUseCase = Mockito.mock(UserUseCase::class.java)
        userController =
            UserController(
                refreshTokenCookieManager = Mockito.mock(RefreshTokenCookieManager::class.java),
                userUseCase = userUseCase,
            )
    }

    @Test
    fun `사용자 ID로 가입일을 조회한다`() {
        val response = UserSignupDateResponse(SIGNUP_DATE)
        Mockito.`when`(userUseCase.getSignupDate(USER_ID)).thenReturn(response)

        val result = userController.getSignupDate(USER_ID)

        assertEquals(response, result)
        Mockito.verify(userUseCase).getSignupDate(USER_ID)
    }

    private companion object {
        const val USER_ID = 1L
        val SIGNUP_DATE: LocalDate = LocalDate.of(2026, 6, 13)
    }
}
