package com.firstpenguin.app.domain.discovery.model

import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode

enum class DiscoveryGenre(
    val value: String,
) {
    GENERAL_LITERATURE("일반문학"),
    SCIENCE_FICTION("SF"),
    MYSTERY("추리･미스터리"),
    HORROR_THRILLER("공포･스릴러"),
    FANTASY("판타지"),
    ROMANCE("로맨스"),
    HISTORICAL("역사"),
    MARTIAL_ARTS("무협"),
    ESSAY_POETRY("시･에세이"),
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
