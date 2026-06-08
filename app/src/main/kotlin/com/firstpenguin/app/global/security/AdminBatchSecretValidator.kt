package com.firstpenguin.app.global.security

import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

const val ADMIN_BATCH_SECRET_HEADER = "X-Admin-Batch-Secret"

@Component
class AdminBatchSecretValidator(
    @Value("\${batch.admin-secret:}") private val adminSecret: String,
) {
    init {
        require(adminSecret.isNotBlank()) { ErrorCode.BATCH_ADMIN_SECRET_REQUIRED.message }
    }

    fun validate(requestSecret: String?) {
        if (requestSecret.isNullOrBlank() || requestSecret != adminSecret) {
            throw CustomException(ErrorCode.UNAUTHORIZED)
        }
    }
}
