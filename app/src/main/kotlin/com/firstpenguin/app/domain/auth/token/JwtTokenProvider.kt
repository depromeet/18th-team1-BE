package com.firstpenguin.app.domain.auth.token

import com.firstpenguin.app.domain.auth.config.AuthProperties
import com.firstpenguin.app.domain.auth.model.TokenClaims
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
            tokenType = ACCESS_TOKEN_TYPE,
            expiration = authProperties.jwt.accessTokenExpiration,
        )

    fun createRefreshToken(userId: Long): String =
        createToken(
            userId = userId,
            tokenType = REFRESH_TOKEN_TYPE,
            expiration = authProperties.jwt.refreshTokenExpiration,
        )

    fun validateAccessToken(token: String): TokenClaims =
        validateToken(
            token = token,
            expectedType = ACCESS_TOKEN_TYPE,
            invalidTokenError = ErrorCode.INVALID_ACCESS_TOKEN,
            tokenExpiredError = ErrorCode.ACCESS_TOKEN_EXPIRED,
        )

    fun validateRefreshToken(token: String): TokenClaims =
        validateToken(
            token = token,
            expectedType = REFRESH_TOKEN_TYPE,
            invalidTokenError = ErrorCode.INVALID_REFRESH_TOKEN,
            tokenExpiredError = ErrorCode.REFRESH_TOKEN_EXPIRED,
        )

    fun hash(token: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(token.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { "%02x".format(it) }

    private fun createToken(
        userId: Long,
        tokenType: String,
        expiration: Duration,
    ): String {
        val now = Instant.now()
        val claims = claims(userId, tokenType, now, expiration)
        val signedJwt = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims)

        signedJwt.sign(MACSigner(signingKey()))
        return signedJwt.serialize()
    }

    private fun validateToken(
        token: String,
        expectedType: String,
        invalidTokenError: ErrorCode,
        tokenExpiredError: ErrorCode,
    ): TokenClaims =
        try {
            val signedJwt = SignedJWT.parse(token)
            validateParsedToken(signedJwt, expectedType, invalidTokenError, tokenExpiredError)
            tokenClaims(signedJwt, invalidTokenError)
        } catch (e: CustomException) {
            throw e
        } catch (e: ParseException) {
            throw CustomException(invalidTokenError).also { it.initCause(e) }
        } catch (e: JOSEException) {
            throw CustomException(invalidTokenError).also { it.initCause(e) }
        } catch (e: IllegalArgumentException) {
            throw CustomException(invalidTokenError).also { it.initCause(e) }
        }

    private fun claims(
        userId: Long,
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
        return builder.build()
    }

    private fun validateParsedToken(
        signedJwt: SignedJWT,
        expectedType: String,
        invalidTokenError: ErrorCode,
        tokenExpiredError: ErrorCode,
    ) {
        val expirationTime = signedJwt.jwtClaimsSet.expirationTime?.toInstant()
        val errorCode =
            when {
                !signedJwt.verify(MACVerifier(signingKey())) -> invalidTokenError
                expirationTime == null || !expirationTime.isAfter(Instant.now()) -> tokenExpiredError
                signedJwt.jwtClaimsSet.getStringClaim(TOKEN_TYPE_CLAIM) != expectedType -> invalidTokenError
                else -> null
            }

        errorCode?.let { throw CustomException(it) }
    }

    private fun tokenClaims(
        signedJwt: SignedJWT,
        invalidTokenError: ErrorCode,
    ): TokenClaims {
        val userId =
            signedJwt.jwtClaimsSet.subject.toLongOrNull()
                ?: throw CustomException(invalidTokenError)

        return TokenClaims(userId = userId)
    }

    private fun signingKey(): ByteArray {
        if (authProperties.jwt.secret.isBlank()) {
            throw CustomException(ErrorCode.INTERNAL_SERVER_ERROR)
        }

        return MessageDigest
            .getInstance("SHA-256")
            .digest(authProperties.jwt.secret.toByteArray(StandardCharsets.UTF_8))
    }

    private companion object {
        const val ACCESS_TOKEN_TYPE = "access"
        const val REFRESH_TOKEN_TYPE = "refresh"
        const val TOKEN_TYPE_CLAIM = "type"
    }
}
