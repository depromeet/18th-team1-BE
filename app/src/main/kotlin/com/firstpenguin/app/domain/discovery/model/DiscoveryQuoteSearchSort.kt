package com.firstpenguin.app.domain.discovery.model

import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode

enum class DiscoveryQuoteSearchSort(
    val value: String,
) {
    LATEST("latest"),
    SCRAP_COUNT("scrap"),
    ;

    companion object {
        fun parse(sort: String?): DiscoveryQuoteSearchSort {
            val normalizedSort = sort?.trim()
            if (normalizedSort.isNullOrBlank()) return LATEST

            return entries.find { entry -> entry.value == normalizedSort }
                ?: throw CustomException(ErrorCode.INVALID_INPUT)
        }
    }
}
