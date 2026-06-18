package com.firstpenguin.app.domain.recommendation.policy

import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.RecommendationFinalScoreWeights
import com.firstpenguin.app.global.enums.TagType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RecommendationFinalScoreWeightPolicyTest {
    @Test
    fun `context situation 입력이 없으면 기본 점수 비율을 사용한다`() {
        val weights =
            RecommendationFinalScoreWeightPolicy.weightsOf(
                effectiveTags = listOf(effectiveTag(EMOTION_TAG_ID, TagType.EMOTION)),
                candidates = listOf(candidate(1L)),
            )

        assertEquals(RecommendationFinalScoreWeights.DEFAULT, weights)
    }

    @Test
    fun `context situation 입력이 있는데 후보군 매칭이 없으면 semantic 비율을 크게 높인다`() {
        val weights =
            RecommendationFinalScoreWeightPolicy.weightsOf(
                effectiveTags = listOf(effectiveTag(SITUATION_TAG_ID, TagType.SITUATION)),
                candidates = (1L..10L).map(::candidate),
            )

        assertEquals(RecommendationFinalScoreWeights.SEMANTIC_FOCUSED, weights)
    }

    @Test
    fun `context situation 후보 매칭 비율이 낮으면 semantic 비율을 일부 높인다`() {
        val weights =
            RecommendationFinalScoreWeightPolicy.weightsOf(
                effectiveTags = listOf(effectiveTag(SITUATION_TAG_ID, TagType.SITUATION)),
                candidates =
                    listOf(candidate(1L, mapOf(TagType.SITUATION to setOf(SITUATION_TAG_ID)))) +
                        (2L..10L).map(::candidate),
            )

        assertEquals(RecommendationFinalScoreWeights.SEMANTIC_LEANING, weights)
    }

    @Test
    fun `context situation 후보 매칭 비율이 충분하면 기본 점수 비율을 유지한다`() {
        val weights =
            RecommendationFinalScoreWeightPolicy.weightsOf(
                effectiveTags = listOf(effectiveTag(SITUATION_TAG_ID, TagType.SITUATION)),
                candidates =
                    (1L..2L).map { quoteId ->
                        candidate(quoteId, mapOf(TagType.SITUATION to setOf(SITUATION_TAG_ID)))
                    } + (3L..10L).map(::candidate),
            )

        assertEquals(RecommendationFinalScoreWeights.DEFAULT, weights)
    }

    private companion object {
        const val BOOK_ID = 10L
        const val EMOTION_TAG_ID = 101L
        const val SITUATION_TAG_ID = 201L

        fun effectiveTag(
            tagId: Long,
            tagType: TagType,
        ): EffectiveTag =
            EffectiveTag(
                tagId = tagId,
                code = "${tagType.name}_$tagId",
                type = tagType,
            )

        fun candidate(
            quoteId: Long,
            tagIdsByType: Map<TagType, Set<Long>> = emptyMap(),
        ): RecommendationCandidate =
            RecommendationCandidate(
                quoteId = quoteId,
                bookId = BOOK_ID,
                content = "content-$quoteId",
                title = "title",
                author = "author",
                roleTagId = null,
                tagIdsByType = tagIdsByType,
            )
    }
}
