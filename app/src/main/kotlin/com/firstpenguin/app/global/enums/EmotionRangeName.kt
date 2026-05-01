package com.firstpenguin.app.global.enums

import com.firstpenguin.app.global.exception.ErrorCode
import com.firstpenguin.app.global.exception.CustomException

enum class EmotionRangeName {
    SAD,
    NORMAL,
    HAPPY;

    companion object {
        fun from(value: String): EmotionRangeName =
            entries.find { it.name == value }
                ?: throw CustomException(ErrorCode.INVALID_EMOTION_RANGE_NAME)
    }
}
