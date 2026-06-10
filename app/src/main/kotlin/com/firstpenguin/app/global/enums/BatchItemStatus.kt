package com.firstpenguin.app.global.enums

import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode

enum class BatchItemStatus {
    PREPARING,
    SUBMITTED,
    SUCCEEDED,
    FAILED,
    ;

    companion object {
        fun from(value: String): BatchItemStatus =
            entries.find { it.name == value.uppercase() }
                ?: throw CustomException(ErrorCode.INVALID_QUOTE_BATCH_ITEMS_STATUS)

        fun activeStatuses(): List<BatchItemStatus> =
            listOf(
                PREPARING,
                SUBMITTED,
            )
    }
}
