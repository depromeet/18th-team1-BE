package com.firstpenguin.app.domain.recommendation.policy

import com.firstpenguin.app.domain.recommendation.model.IntentType
import com.firstpenguin.app.global.enums.TagType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IntentFocusWeightPolicyTest {
    @Test
    fun `모든 intent type에서 emotion weight는 need weight보다 크다`() {
        IntentType.entries.forEach { intentType ->
            val weights = IntentFocusWeightPolicy.weightsOf(intentType)

            assertTrue(weights.getValue(TagType.EMOTION) > weights.getValue(TagType.NEED))
        }
    }

    @Test
    fun `모든 intent type의 focus weight 합은 1이다`() {
        IntentType.entries.forEach { intentType ->
            val weights = IntentFocusWeightPolicy.weightsOf(intentType)

            assertEquals(FULL_WEIGHT, weights.values.sum(), DELTA)
        }
    }

    private companion object {
        const val FULL_WEIGHT = 1.0
        const val DELTA = 0.000001
    }
}
