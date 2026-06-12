package com.firstpenguin.app.domain.discovery.model

import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode

enum class DiscoveryGenre(
    val value: String,
) {
    KOREAN_NOVEL("한국소설"),
    JAPANESE_NOVEL("일본소설"),
    ENGLISH_NOVEL("영미소설"),
    FANTASY("판타지"),
    CLASSIC_LITERATURE("고전문학"),
    HUMANITIES("인문"),
    PHILOSOPHY("철학"),
    ESSAY_POETRY("에세이•시"),
    MOVIE_DRAMA_ORIGINAL("영화•드라마 원작"),
    ;

    companion object {
        fun parse(genre: String?): DiscoveryGenre? {
            val normalizedGenre = genre?.trim()
            if (normalizedGenre.isNullOrBlank() || normalizedGenre == ALL_GENRE) return null

            return entries.find { entry -> entry.value == normalizedGenre }
                ?: throw CustomException(ErrorCode.INVALID_INPUT)
        }

        private const val ALL_GENRE = "전체"
    }
}
