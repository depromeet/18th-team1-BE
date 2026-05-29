package com.firstpenguin.app.domain.batch.service

import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AdminBatchSecretValidator(
    @Value("\${batch.admin-secret}") private val adminSecret: String,
) {
    fun validate(requestSecret: String?) {
        if (requestSecret.isNullOrBlank() || requestSecret != adminSecret) {
            throw CustomException(ErrorCode.UNAUTHORIZED)
        }
    }
}
