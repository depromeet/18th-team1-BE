package com.firstpenguin.app.domain.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

private const val DEFAULT_ACCESS_TOKEN_MINUTES = 30L
private const val DEFAULT_REFRESH_TOKEN_DAYS = 7L

@ConfigurationProperties(prefix = "auth")
data class AuthProperties(
    var jwt: Jwt = Jwt(),
    var refreshToken: RefreshToken = RefreshToken(),
    var oauth2: OAuth2 = OAuth2(),
) {
    data class Jwt(
        var secret: String = "",
        var accessTokenExpiration: Duration = Duration.ofMinutes(DEFAULT_ACCESS_TOKEN_MINUTES),
        var refreshTokenExpiration: Duration = Duration.ofDays(DEFAULT_REFRESH_TOKEN_DAYS),
    )

    data class RefreshToken(
        var cookieName: String = "refresh_token",
        var cookiePath: String = "/api/auth",
        var secure: Boolean = true,
    )

    data class OAuth2(
        var successRedirectUrl: String = "http://localhost:3000/auth/callback",
        var failureRedirectUrl: String = "http://localhost:3000/login",
    )
}
