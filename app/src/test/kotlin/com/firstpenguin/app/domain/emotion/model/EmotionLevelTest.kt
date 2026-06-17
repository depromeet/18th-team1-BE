package com.firstpenguin.app.domain.emotion.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EmotionLevelTest {
    @Test
    fun `감정 단계 값으로 표시 문구를 조회한다`() {
        val labels =
            (1..9).map { value ->
                EmotionLevel.from(value).label
            }

        assertEquals(
            listOf(
                "아주 별로에요",
                "별로에요",
                "약간 별로에요",
                "그저그래요",
                "나쁘지 않아요",
                "꽤 괜찮아요",
                "약간 기분 좋아요",
                "기분 좋아요",
                "아주 기분 좋아요!",
            ),
            labels,
        )
    }
}
