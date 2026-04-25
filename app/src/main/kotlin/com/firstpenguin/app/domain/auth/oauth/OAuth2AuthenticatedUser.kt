package com.firstpenguin.app.domain.auth.oauth

import com.firstpenguin.app.domain.user.model.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

data class OAuth2AuthenticatedUser(
    val user: User,
    private val attributes: Map<String, Any>,
) : OAuth2User {
    private val roleAuthority = SimpleGrantedAuthority("ROLE_${user.role.name}")

    override fun getName(): String = user.id.toString()

    override fun getAttributes(): Map<String, Any> = attributes

    override fun getAuthorities(): Collection<GrantedAuthority> = listOf(roleAuthority)
}
