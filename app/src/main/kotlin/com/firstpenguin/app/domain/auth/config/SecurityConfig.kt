package com.firstpenguin.app.domain.auth.config

import com.firstpenguin.app.domain.auth.oauth.CustomOAuth2UserService
import com.firstpenguin.app.domain.auth.oauth.CustomOidcUserService
import com.firstpenguin.app.domain.auth.oauth.JwtIssueSuccessHandler
import com.firstpenguin.app.domain.auth.oauth.OAuth2FailureHandler
import com.firstpenguin.app.domain.auth.token.JWT_AUTHENTICATION_ERROR_ATTRIBUTE
import com.firstpenguin.app.domain.auth.token.JwtAuthenticationFilter
import com.firstpenguin.app.global.exception.ErrorCode
import com.firstpenguin.app.global.response.ErrorResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import tools.jackson.databind.json.JsonMapper

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val customOidcUserService: CustomOidcUserService,
    private val jwtIssueSuccessHandler: JwtIssueSuccessHandler,
    private val oAuth2FailureHandler: OAuth2FailureHandler,
    private val jsonMapper: JsonMapper,
    private val authProperties: AuthProperties,
) {
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        clientRegistrationRepository: ObjectProvider<ClientRegistrationRepository>,
    ): SecurityFilterChain {
        http.cors { cors -> cors.configurationSource(corsConfigurationSource()) }
        http.csrf { csrf -> csrf.disable() }
        http.sessionManagement { session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) }
        http.authorizeHttpRequests { authorize ->
            authorize.requestMatchers(*PERMIT_ALL_PATTERNS).permitAll()
            authorize.anyRequest().authenticated()
        }
        http.exceptionHandling { exceptions ->
            exceptions.authenticationEntryPoint { request, response, _ ->
                val errorCode =
                    request.getAttribute(JWT_AUTHENTICATION_ERROR_ATTRIBUTE) as? ErrorCode
                        ?: ErrorCode.UNAUTHORIZED
                writeError(response, errorCode)
            }
            exceptions.accessDeniedHandler { _, response, _ -> writeError(response, ErrorCode.FORBIDDEN) }
        }
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        clientRegistrationRepository.ifAvailable { configureOAuth2Login(http) }

        return http.build()
    }

    private fun configureOAuth2Login(http: HttpSecurity) {
        http.oauth2Login { oauth2 ->
            oauth2.userInfoEndpoint { userInfo ->
                userInfo.userService(customOAuth2UserService)
                userInfo.oidcUserService(customOidcUserService)
            }
            oauth2.successHandler(jwtIssueSuccessHandler)
            oauth2.failureHandler(oAuth2FailureHandler)
        }
    }

    private fun writeError(
        response: HttpServletResponse,
        errorCode: ErrorCode,
    ) {
        response.status = errorCode.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        jsonMapper.writeValue(response.writer, ErrorResponse.of(errorCode))
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config =
            CorsConfiguration().apply {
                allowedOrigins = authProperties.oauth2.allowedOrigins
                allowedMethods = CORS_ALLOWED_METHODS
                allowedHeaders = listOf("*")
                allowCredentials = true
            }

        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/**", config) }
    }

    private companion object {
        val CORS_ALLOWED_METHODS =
            listOf(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name(),
            )

        val PERMIT_ALL_PATTERNS =
            arrayOf(
                "/auth/refresh",
                "/auth/logout",
                "/auth/dev-token",
                "/oauth2/**",
                "/login/oauth2/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**",
                "/error",
                "/actuator/health",
            )
    }
}
