package com.firstpenguin.app.global.enums

import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode

enum class TagType {
    EMOTION,
    TONE,
    ;

    companion object {
        fun from(value: String): TagType =
            runCatching { valueOf(value.uppercase()) }
                .getOrElse { throw CustomException(ErrorCode.INVALID_TAG_TYPE) }
    }
}
