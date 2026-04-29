package com.firstpenguin.app.domain.auth.oauth

import com.firstpenguin.app.domain.user.model.OAuthUserProfile
import com.firstpenguin.app.domain.user.model.Provider
import com.firstpenguin.app.domain.user.repository.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository,
) : OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private val delegate = DefaultOAuth2UserService()

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = delegate.loadUser(userRequest)
        val registrationId = userRequest.clientRegistration.registrationId.lowercase()
        val profile = toUserProfile(registrationId, oAuth2User.attributes)
        val user = userRepository.upsertOAuthUser(profile)

        return OAuth2AuthenticatedUser(user = user, attributes = oAuth2User.attributes)
    }

    private fun toUserProfile(
        registrationId: String,
        attributes: Map<String, Any>,
    ): OAuthUserProfile =
        when (registrationId) {
            "kakao" -> kakaoProfile(attributes)
            "google" -> googleProfile(attributes)
            else -> throw providerException()
        }

    private fun kakaoProfile(attributes: Map<String, Any>): OAuthUserProfile {
        val externalId = attributes["id"]?.toString() ?: throw providerException()
        val account = attributes.mapValue("kakao_account")
        val profile = account.mapValue("profile")
        val nickname = profile["nickname"] as? String ?: attributes.mapValue("properties")["nickname"] as? String
        val profileImageUrl = profile["profile_image_url"] as? String ?: profile["thumbnail_image_url"] as? String

        return OAuthUserProfile(
            provider = Provider.KAKAO,
            providerId = "kakao_$externalId",
            email = null,
            nickname = normalizeNickname(Provider.KAKAO, nickname, externalId),
            profileImageKey = profileImageUrl,
        )
    }

    private fun googleProfile(attributes: Map<String, Any>): OAuthUserProfile {
        val externalId = attributes["sub"]?.toString() ?: throw providerException()
        val nickname = attributes["name"] as? String

        return OAuthUserProfile(
            provider = Provider.GOOGLE,
            providerId = "google_$externalId",
            email = attributes["email"] as? String,
            nickname = normalizeNickname(Provider.GOOGLE, nickname, externalId),
            profileImageKey = null,
        )
    }

    private fun normalizeNickname(
        provider: Provider,
        nickname: String?,
        externalId: String,
    ): String {
        val normalized = nickname.orEmpty().replace(WHITESPACE_REGEX, "").take(NICKNAME_MAX_LENGTH)

        if (normalized.isNotBlank()) {
            return normalized
        }

        return "${provider.name.lowercase()}${externalId.takeLast(FALLBACK_SUFFIX_LENGTH)}".take(NICKNAME_MAX_LENGTH)
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.mapValue(key: String): Map<String, Any> = this[key] as? Map<String, Any> ?: emptyMap()

    private fun providerException(): OAuth2AuthenticationException =
        OAuth2AuthenticationException(OAuth2Error("unsupported_oauth_provider"))

    private companion object {
        const val NICKNAME_MAX_LENGTH = 15
        const val FALLBACK_SUFFIX_LENGTH = 6
        val WHITESPACE_REGEX = Regex("\\s+")
    }
}
