package com.firstpenguin.app.domain.batch.service

import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private const val ADMIN_BATCH_SECRET_REQUIRED_MESSAGE = "batch.admin-secret must not be blank"

@Component
class AdminBatchSecretValidator(
    @Value("\${batch.admin-secret:}") private val adminSecret: String,
) {
    init {
        require(adminSecret.isNotBlank()) { ADMIN_BATCH_SECRET_REQUIRED_MESSAGE }
    }

    fun validate(requestSecret: String?) {
        if (requestSecret.isNullOrBlank() || requestSecret != adminSecret) {
            throw CustomException(ErrorCode.UNAUTHORIZED)
        }
    }
}
