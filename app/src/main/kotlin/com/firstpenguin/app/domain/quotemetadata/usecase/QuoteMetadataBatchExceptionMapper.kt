package com.firstpenguin.app.domain.quotemetadata.usecase

import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.web.client.RestClientException

internal fun Throwable.toQuoteMetadataBatchException(): Throwable {
    if (this is RestClientException) {
        return CustomException(ErrorCode.QUOTE_METADATA_BATCH_OPENAI_REQUEST_FAILED)
    }

    return this
}
