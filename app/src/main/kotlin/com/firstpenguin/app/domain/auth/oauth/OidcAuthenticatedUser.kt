package com.firstpenguin.app.domain.auth.oauth

import com.firstpenguin.app.domain.user.model.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser

class OidcAuthenticatedUser(
    val user: User,
    idToken: OidcIdToken,
    userInfo: OidcUserInfo?,
    authorities: Collection<GrantedAuthority> = emptyList(),
) : DefaultOidcUser(authorities, idToken, userInfo)
