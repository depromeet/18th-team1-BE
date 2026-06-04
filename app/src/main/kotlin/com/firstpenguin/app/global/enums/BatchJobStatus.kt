package com.firstpenguin.app.global.enums

import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode

enum class BatchJobStatus {
    PREPARING,
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

    fun isTerminal(): Boolean =
        this in
            listOf(
                COMPLETED,
                FAILED,
                EXPIRED,
                CANCELLED,
            )

    fun isFailedTerminal(): Boolean =
        this in
            listOf(
                FAILED,
                EXPIRED,
                CANCELLED,
            )

    companion object {
        fun from(value: String): BatchJobStatus =
            entries.find { it.name == value.uppercase() }
                ?: throw CustomException(ErrorCode.INVALID_QUOTE_METADATA_BATCH_STATUS)

        fun runningStatuses(): List<BatchJobStatus> =
            listOf(
                PREPARING,
                SUBMITTED,
                VALIDATING,
                IN_PROGRESS,
                FINALIZING,
                CANCELLING,
            )
    }
}
