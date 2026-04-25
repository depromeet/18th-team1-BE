package com.firstpenguin.app.domain.auth.token

import com.firstpenguin.app.domain.auth.config.AuthProperties
import com.firstpenguin.app.domain.auth.model.TokenClaims
import com.firstpenguin.app.domain.user.model.Role
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.ParseException
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID

@Component
class JwtTokenProvider(
    private val authProperties: AuthProperties,
) {
    fun createAccessToken(user: User): String =
        createToken(
            userId = user.id,
            role = user.role,
            tokenType = ACCESS_TOKEN_TYPE,
            expiration = authProperties.jwt.accessTokenExpiration,
        )

    fun createRefreshToken(userId: Long): String =
        createToken(
            userId = userId,
            role = null,
            tokenType = REFRESH_TOKEN_TYPE,
            expiration = authProperties.jwt.refreshTokenExpiration,
        )

    fun validateAccessToken(token: String): TokenClaims = validateToken(token, ACCESS_TOKEN_TYPE)

    fun validateRefreshToken(token: String): TokenClaims = validateToken(token, REFRESH_TOKEN_TYPE)

    fun hash(token: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(token.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { "%02x".format(it) }

    private fun createToken(
        userId: Long,
        role: Role?,
        tokenType: String,
        expiration: Duration,
    ): String {
        val now = Instant.now()
        val claims = claims(userId, role, tokenType, now, expiration)
        val signedJwt = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims)

        signedJwt.sign(MACSigner(signingKey()))
        return signedJwt.serialize()
    }

    private fun validateToken(
        token: String,
        expectedType: String,
    ): TokenClaims =
        try {
            val signedJwt = SignedJWT.parse(token)
            validateParsedToken(signedJwt, expectedType)
            tokenClaims(signedJwt)
        } catch (e: CustomException) {
            throw e
        } catch (e: ParseException) {
            throw CustomException(ErrorCode.INVALID_TOKEN).also { it.initCause(e) }
        } catch (e: JOSEException) {
            throw CustomException(ErrorCode.INVALID_TOKEN).also { it.initCause(e) }
        } catch (e: IllegalArgumentException) {
            throw CustomException(ErrorCode.INVALID_TOKEN).also { it.initCause(e) }
        }

    private fun claims(
        userId: Long,
        role: Role?,
        tokenType: String,
        now: Instant,
        expiration: Duration,
    ): JWTClaimsSet {
        val builder =
            JWTClaimsSet
                .Builder()
                .subject(userId.toString())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(expiration)))
                .claim(TOKEN_TYPE_CLAIM, tokenType)

        role?.let { builder.claim(ROLE_CLAIM, it.name) }
        return builder.build()
    }

    private fun validateParsedToken(
        signedJwt: SignedJWT,
        expectedType: String,
    ) {
        val expirationTime = signedJwt.jwtClaimsSet.expirationTime?.toInstant()
        val errorCode =
            when {
                !signedJwt.verify(MACVerifier(signingKey())) -> ErrorCode.INVALID_TOKEN
                expirationTime == null || !expirationTime.isAfter(Instant.now()) -> ErrorCode.TOKEN_EXPIRED
                signedJwt.jwtClaimsSet.getStringClaim(TOKEN_TYPE_CLAIM) != expectedType -> ErrorCode.INVALID_TOKEN
                else -> null
            }

        errorCode?.let { throw CustomException(it) }
    }

    private fun tokenClaims(signedJwt: SignedJWT): TokenClaims {
        val userId =
            signedJwt.jwtClaimsSet.subject.toLongOrNull()
                ?: throw CustomException(ErrorCode.INVALID_TOKEN)
        val role = signedJwt.jwtClaimsSet.getStringClaim(ROLE_CLAIM)?.let(Role::valueOf)

        return TokenClaims(userId = userId, role = role)
    }

    private fun signingKey(): ByteArray {
        if (authProperties.jwt.secret.isBlank()) {
            throw CustomException(ErrorCode.INVALID_TOKEN)
        }

        return MessageDigest
            .getInstance("SHA-256")
            .digest(authProperties.jwt.secret.toByteArray(StandardCharsets.UTF_8))
    }

    private companion object {
        const val ACCESS_TOKEN_TYPE = "access"
        const val REFRESH_TOKEN_TYPE = "refresh"
        const val TOKEN_TYPE_CLAIM = "type"
        const val ROLE_CLAIM = "role"
    }
}
