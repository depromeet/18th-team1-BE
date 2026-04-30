package com.firstpenguin.app.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun openApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("First Penguin API")
                    .description("OAuth 로그인, JWT 인증, 사용자 API 문서입니다.")
                    .version("v1"),
            ).servers(
                listOf(
                    Server()
                        .url("/api")
                        .description("API prefix"),
                ),
            ).components(
                Components().addSecuritySchemes(
                    BEARER_AUTH,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                ),
            )

    @Bean
    fun oauth2AuthorizationPathCustomizer(): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            openApi.path(
                "/oauth2/authorization/kakao",
                PathItem()
                    .get(
                        Operation()
                            .tags(listOf("인증"))
                            .summary("카카오 로그인 시작")
                            .description(OAUTH2_AUTHORIZATION_DESCRIPTION)
                            .responses(
                                ApiResponses()
                                    .addApiResponse(
                                        "302",
                                        ApiResponse().description("카카오 로그인 페이지로 redirect 됩니다."),
                                    ),
                            ),
                    ),
            )
        }

    private companion object {
        const val BEARER_AUTH = "bearerAuth"
        const val OAUTH2_AUTHORIZATION_DESCRIPTION =
            "프론트 카카오 로그인 버튼은 브라우저를 공개 경로 " +
                "`/api/oauth2/authorization/kakao`로 이동시킵니다. " +
                "백엔드는 카카오 로그인 페이지로 redirect하고, 로그인 성공 후 " +
                "`/api/login/oauth2/code/kakao` callback을 처리합니다."
    }
}
