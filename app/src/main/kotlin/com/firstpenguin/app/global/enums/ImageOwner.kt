package com.firstpenguin.app.global.enums

import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode

enum class ImageOwner {
    USER,
    DIARY,
    BOOK,
    REPORT,
    ;

    companion object {
        fun from(value: String): ImageOwner =
            runCatching { valueOf(value.uppercase()) }
                .getOrElse { throw CustomException(ErrorCode.INVALID_IMAGE_OWNER_TYPE) }
    }
}
