package com.firstpenguin.app.global.enums

import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode

enum class TagPriority(
    val code: String,
) {
    PRIMARY("primary"),
    SECONDARY("secondary"),
    BACKGROUND("background"),
    ;

    companion object {
        fun from(value: String): TagPriority =
            entries
                .find { priority -> priority.code == value.lowercase() }
                ?: throw CustomException(ErrorCode.INVALID_TAG_PRIORITY)

        fun codes(): List<String> = entries.map { priority -> priority.code }
    }
}
