package com.firstpenguin.app.global.enums

import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode

enum class BatchJobStatus {
    SUBMITTED,
    VALIDATING,
    IN_PROGRESS,
    FINALIZING,
    COMPLETED,
    FAILED,
    EXPIRED,
    CANCELLING,
    CANCELLED,
    ;

    companion object {
        fun from(value: String): BatchJobStatus =
            entries.find { it.name == value }
                ?: throw CustomException(ErrorCode.INVALID_QUOTE_METADATA_BATCH_STATUS)
    }
}
