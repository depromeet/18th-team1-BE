package com.firstpenguin.app.domain.auth.config

import com.firstpenguin.app.domain.auth.oauth.CustomOAuth2UserService
import com.firstpenguin.app.domain.auth.oauth.CustomOidcUserService
import com.firstpenguin.app.domain.auth.oauth.JwtIssueSuccessHandler
import com.firstpenguin.app.domain.auth.oauth.OAuth2FailureHandler
import com.firstpenguin.app.domain.auth.token.JwtAuthenticationFilter
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpMethod
import org.springframework.mock.web.MockHttpServletRequest
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SecurityConfigTest {
    @Test
    fun `CORS 허용 메서드에 PATCH를 포함한다`() {
        val source = securityConfig().corsConfigurationSource()
        val request = MockHttpServletRequest(HttpMethod.OPTIONS.name(), "/quotes/1")
        val config = source.getCorsConfiguration(request)

        assertNotNull(config)
        assertTrue(config.allowedMethods.orEmpty().contains(HttpMethod.PATCH.name()))
    }

    private fun securityConfig(): SecurityConfig =
        SecurityConfig(
            jwtAuthenticationFilter = Mockito.mock(JwtAuthenticationFilter::class.java),
            customOAuth2UserService = Mockito.mock(CustomOAuth2UserService::class.java),
            customOidcUserService = Mockito.mock(CustomOidcUserService::class.java),
            jwtIssueSuccessHandler = Mockito.mock(JwtIssueSuccessHandler::class.java),
            oAuth2FailureHandler = Mockito.mock(OAuth2FailureHandler::class.java),
            jsonMapper = JsonMapper.builder().build(),
            authProperties = authProperties(),
        )

    private fun authProperties(): AuthProperties =
        AuthProperties(
            jwt = AuthProperties.Jwt(secret = "secret"),
            oauth2 = AuthProperties.OAuth2(allowedOrigins = listOf("https://example.com")),
        )
}
