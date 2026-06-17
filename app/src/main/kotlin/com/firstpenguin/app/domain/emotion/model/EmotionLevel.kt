package com.firstpenguin.app.domain.emotion.model

enum class EmotionLevel(
    val value: Int,
    val label: String,
) {
    VERY_BAD(1, "아주 별로에요"),
    BAD(2, "별로에요"),
    SLIGHTLY_BAD(3, "약간 별로에요"),
    SO_SO(4, "그저그래요"),
    NOT_BAD(5, "나쁘지 않아요"),
    PRETTY_GOOD(6, "꽤 괜찮아요"),
    SLIGHTLY_HAPPY(7, "약간 기분 좋아요"),
    HAPPY(8, "기분 좋아요"),
    VERY_HAPPY(9, "아주 기분 좋아요!"),
    ;

    companion object {
        fun from(value: Int): EmotionLevel =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("Invalid emotion value: $value")
    }
}
