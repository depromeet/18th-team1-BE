package com.firstpenguin.app.domain.auth.oauth

import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.Provider
import com.firstpenguin.app.domain.user.usecase.OAuthUserUseCase
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service

@Service
class CustomOidcUserService(
    private val oAuthUserUseCase: OAuthUserUseCase,
) : OidcUserService() {
    override fun loadUser(userRequest: OidcUserRequest): OidcUser {
        val oidcUser = super.loadUser(userRequest)
        val externalId = oidcUser.subject
        val nickname = oidcUser.fullName

        val profile =
            OAuthUserProfile(
                provider = Provider.GOOGLE,
                providerId = externalId,
                email = oidcUser.email,
                nickname = normalizeNickname(nickname, externalId),
            )
        val user = oAuthUserUseCase.upsertOAuthUser(profile)

        return OidcAuthenticatedUser(user = user, idToken = oidcUser.idToken, userInfo = oidcUser.userInfo)
    }

    private fun normalizeNickname(
        nickname: String?,
        externalId: String,
    ): String {
        val normalized = nickname.orEmpty().replace(WHITESPACE_REGEX, "").take(NICKNAME_MAX_LENGTH)
        if (normalized.isNotBlank()) return normalized
        return "google${externalId.takeLast(FALLBACK_SUFFIX_LENGTH)}".take(NICKNAME_MAX_LENGTH)
    }

    private companion object {
        const val NICKNAME_MAX_LENGTH = 15
        const val FALLBACK_SUFFIX_LENGTH = 6
        val WHITESPACE_REGEX = Regex("\\s+")
    }
}
