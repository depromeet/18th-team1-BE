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
                providerDisplayName = providerDisplayName(nickname, externalId),
            )
        val user = oAuthUserUseCase.loginOAuthUser(profile)

        return OidcAuthenticatedUser(user = user, idToken = oidcUser.idToken, userInfo = oidcUser.userInfo)
    }

    private fun providerDisplayName(
        displayName: String?,
        externalId: String,
    ): String {
        val normalized = displayName?.trim()?.take(PROVIDER_DISPLAY_NAME_MAX_LENGTH)
        if (!normalized.isNullOrBlank()) return normalized
        return "google${externalId.takeLast(FALLBACK_SUFFIX_LENGTH)}"
    }

    private companion object {
        const val PROVIDER_DISPLAY_NAME_MAX_LENGTH = 100
        const val FALLBACK_SUFFIX_LENGTH = 6
    }
}
