package com.firstpenguin.app.domain.auth.model

import com.firstpenguin.app.domain.user.model.Role
import org.springframework.security.core.authority.SimpleGrantedAuthority

data class AuthenticatedUser(
    val id: Long,
    val role: Role,
) {
    fun authorities(): List<SimpleGrantedAuthority> = listOf(SimpleGrantedAuthority("ROLE_${role.name}"))
}
