package com.firstpenguin.app.global.enums

import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode

enum class BatchItemStatus {
    SUBMITTED,
    SUCCEEDED,
    FAILED,
    ;

    companion object {
        fun from(value: String): BatchItemStatus =
            entries.find { it.name == value }
                ?: throw CustomException(ErrorCode.INVALID_QUOTE_METADATA_BATCH_ITEMS_STATUS)
    }
}
