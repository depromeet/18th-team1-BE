package com.firstpenguin.app.domain.auth.token

import com.firstpenguin.app.domain.auth.config.AuthProperties
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RefreshTokenCookieManagerTest {
    @Test
    fun `refresh token 쿠키는 설정된 SameSite 값을 사용한다`() {
        val authProperties =
            AuthProperties(
                jwt = AuthProperties.Jwt(secret = "secret"),
                refreshToken = AuthProperties.RefreshToken(sameSite = "None"),
            )

        val cookie = RefreshTokenCookieManager(authProperties).create("refresh-token")

        assertEquals("None", cookie.sameSite)
    }
}
