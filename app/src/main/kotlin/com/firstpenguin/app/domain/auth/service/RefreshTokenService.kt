package com.firstpenguin.app.domain.auth.service

import com.firstpenguin.app.domain.auth.config.AuthProperties
import com.firstpenguin.app.domain.auth.model.TokenPair
import com.firstpenguin.app.domain.auth.repository.RefreshTokenRepository
import com.firstpenguin.app.domain.auth.token.JwtTokenProvider
import com.firstpenguin.app.domain.user.model.User
import com.firstpenguin.app.domain.user.repository.UserRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val authProperties: AuthProperties,
) {
    @Transactional
    fun issue(user: User): String {
        val refreshToken = jwtTokenProvider.createRefreshToken(user.id)

        refreshTokenRepository.save(
            userId = user.id,
            deviceId = UUID.randomUUID().toString(),
            tokenHash = jwtTokenProvider.hash(refreshToken),
            expiresAt = refreshTokenExpiresAt(),
        )
        return refreshToken
    }

    @Transactional
    fun rotate(refreshToken: String): TokenPair {
        val claims = jwtTokenProvider.validateRefreshToken(refreshToken)
        val storedToken = findStoredTokenOrDeleteAll(refreshToken, claims.userId)
        validateStoredToken(storedToken.id, storedToken.expiresAt)

        val user = userRepository.findById(claims.userId) ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        val newRefreshToken = jwtTokenProvider.createRefreshToken(user.id)
        refreshTokenRepository.updateToken(
            id = storedToken.id,
            tokenHash = jwtTokenProvider.hash(newRefreshToken),
            expiresAt = refreshTokenExpiresAt(),
        )

        return TokenPair(
            accessToken = jwtTokenProvider.createAccessToken(user),
            refreshToken = newRefreshToken,
        )
    }

    @Transactional
    fun logout(refreshToken: String) {
        refreshTokenRepository.deleteByTokenHash(jwtTokenProvider.hash(refreshToken))
    }

    private fun findStoredTokenOrDeleteAll(
        refreshToken: String,
        userId: Long,
    ) = refreshTokenRepository.findByTokenHash(jwtTokenProvider.hash(refreshToken))
        ?: run {
            refreshTokenRepository.deleteByUserId(userId)
            throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)
        }

    private fun validateStoredToken(
        tokenId: Long,
        expiresAt: LocalDateTime,
    ) {
        if (expiresAt.isAfter(LocalDateTime.now())) {
            return
        }

        refreshTokenRepository.deleteById(tokenId)
        throw CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED)
    }

    private fun refreshTokenExpiresAt(): LocalDateTime = LocalDateTime.now().plusSeconds(refreshTokenExpirationSeconds)

    private val refreshTokenExpirationSeconds: Long
        get() = authProperties.jwt.refreshTokenExpiration.seconds
}
