package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.RecommendationScoreBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RecommendationRankerTest {
    private val ranker = RecommendationRanker()

    @Test
    fun `metadata score와 semantic score를 합산해 finalScore를 계산한다`() {
        val candidate = candidate(QUOTE_ID)

        val result =
            ranker.rank(listOf(candidate)) {
                score(
                    metadataScore = METADATA_SCORE,
                    semanticScore = SEMANTIC_SCORE,
                )
            }

        assertEquals(EXPECTED_FINAL_SCORE, result.first().score.finalScore, DELTA)
    }

    @Test
    fun `semantic score를 사용하지 않으면 metadata score를 finalScore로 사용한다`() {
        val candidate = candidate(QUOTE_ID)

        val result =
            ranker.rank(
                candidates = listOf(candidate),
                useSemanticScore = false,
            ) {
                score(
                    metadataScore = METADATA_SCORE,
                    semanticScore = 0.0,
                )
            }

        assertEquals(METADATA_SCORE, result.first().score.finalScore, DELTA)
    }

    @Test
    fun `finalScore 기준으로 정렬하고 rank를 다시 부여한다`() {
        val lowScoreCandidate = candidate(quoteId = 1L)
        val highScoreCandidate = candidate(quoteId = 2L)

        val result =
            ranker.rank(listOf(lowScoreCandidate, highScoreCandidate)) { candidate ->
                when (candidate.quoteId) {
                    highScoreCandidate.quoteId -> score(metadataScore = 1.0, semanticScore = 0.0)
                    else -> score(metadataScore = 0.1, semanticScore = 0.9)
                }
            }

        assertEquals(listOf(2L, 1L), result.map { quote -> quote.quoteId })
        assertEquals(listOf(1, 2), result.map { quote -> quote.rank })
    }

    private companion object {
        const val QUOTE_ID = 1L
        const val BOOK_ID = 100L
        const val METADATA_SCORE = 0.8
        const val SEMANTIC_SCORE = 0.6
        const val EXPECTED_FINAL_SCORE = 0.71
        const val DELTA = 0.000001

        fun candidate(quoteId: Long): RecommendationCandidate =
            RecommendationCandidate(
                quoteId = quoteId,
                bookId = BOOK_ID,
                content = "content-$quoteId",
                title = "title",
                author = "author",
                roleTagId = null,
                tagIdsByType = emptyMap(),
            )

        fun score(
            metadataScore: Double,
            semanticScore: Double,
        ): RecommendationScoreBreakdown =
            RecommendationScoreBreakdown(
                needScore = 0.0,
                emotionScore = 0.0,
                contextScore = 0.0,
                situationScore = 0.0,
                moodScore = 0.0,
                metadataScore = metadataScore,
                semanticScore = semanticScore,
                finalScore = 0.0,
            )
    }
}
