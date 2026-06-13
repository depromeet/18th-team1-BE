package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.emotion.model.Tag
import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.IntentType
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.UserInputAnalysis
import com.firstpenguin.app.domain.recommendation.policy.MoodTagPolicy
import com.firstpenguin.app.global.enums.TagType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MetadataScorerTest {
    private val scorer = MetadataScorer(MoodTagPolicy(), TypeScoreCalculator())

    @Test
    fun `metadataScore 기준으로 후보를 정렬한다`() {
        val input = recommendationInput(intentType = IntentType.EMOTION_NEED_BASED)
        val effectiveTags =
            listOf(
                effectiveTag(NEED_COMFORT_ID, "NEED_COMFORT", TagType.NEED, 1.0),
                effectiveTag(EMOTION_ANXIOUS_ID, "EMOTION_ANXIOUS", TagType.EMOTION, 1.0),
                effectiveTag(SITUATION_FAILURE_ID, "SITUATION_FAILURE_MISTAKE", TagType.SITUATION, 0.8),
                effectiveTag(CONTEXT_RAIN_ID, "CONTEXT_RAIN", TagType.CONTEXT, 0.4),
            )
        val highMatchCandidate =
            candidate(
                quoteId = 1L,
                tagIdsByType =
                    mapOf(
                        TagType.NEED to setOf(NEED_COMFORT_ID),
                        TagType.EMOTION to setOf(EMOTION_ANXIOUS_ID),
                        TagType.MOOD to setOf(MOOD_WARM_ID, MOOD_CALM_TONE_ID),
                    ),
            )
        val lowMatchCandidate =
            candidate(
                quoteId = 2L,
                tagIdsByType =
                    mapOf(
                        TagType.SITUATION to setOf(SITUATION_FAILURE_ID),
                        TagType.CONTEXT to setOf(CONTEXT_RAIN_ID),
                    ),
            )

        val result =
            scorer.rank(
                input = input,
                effectiveTags = effectiveTags,
                candidates = listOf(lowMatchCandidate, highMatchCandidate),
                moodTagIdByCode = moodTagIdByCode,
            )

        assertEquals(listOf(1L, 2L), result.map { quote -> quote.quoteId })
        assertEquals(1, result.first().rank)
        assertEquals(result.first().score.metadataScore, result.first().score.finalScore, DELTA)
        assertEquals(0.0, result.first().score.semanticScore, DELTA)
    }

    @Test
    fun `moodScore는 mood 정책 결과와 후보 mood tag 매칭으로 계산한다`() {
        val input = recommendationInput(intentType = IntentType.EMOTION_NEED_BASED)
        val candidate =
            candidate(
                quoteId = 1L,
                tagIdsByType = mapOf(TagType.MOOD to setOf(MOOD_WARM_ID, MOOD_CALM_TONE_ID)),
            )

        val result =
            scorer.score(
                input = input,
                effectiveTags = emptyList(),
                candidate = candidate,
                moodTagIdByCode = moodTagIdByCode,
            )

        assertTrue(result.moodScore > 0.0)
    }

    @Test
    fun `추천 결과는 첫 번째 정렬 후보를 mainQuote로 사용한다`() {
        val result =
            scorer.recommend(
                input = recommendationInput(intentType = IntentType.EMOTION_NEED_BASED),
                effectiveTags = listOf(effectiveTag(NEED_COMFORT_ID, "NEED_COMFORT", TagType.NEED, 1.0)),
                candidates =
                    listOf(
                        candidate(quoteId = 2L, tagIdsByType = emptyMap()),
                        candidate(quoteId = 1L, tagIdsByType = mapOf(TagType.NEED to setOf(NEED_COMFORT_ID))),
                    ),
                moodTagIdByCode = moodTagIdByCode,
            )

        assertEquals(1L, result?.mainQuote?.quoteId)
        assertEquals(listOf(1L, 2L), result?.quotes?.map { quote -> quote.quoteId })
    }

    private fun recommendationInput(intentType: IntentType): RecommendationInput =
        RecommendationInput(
            userId = USER_ID,
            emotionRangeId = EMOTION_RANGE_ID,
            emotionTags = listOf(tag(EMOTION_ANXIOUS_ID, TagType.EMOTION, "EMOTION_ANXIOUS")),
            needTag = tag(NEED_COMFORT_ID, TagType.NEED, "NEED_COMFORT"),
            feelingText = null,
            diaryText = null,
            analysis = UserInputAnalysis(intentType = intentType, canonicalIntent = null, tagCandidates = emptyList()),
        )

    private fun effectiveTag(
        tagId: Long,
        code: String,
        type: TagType,
        importance: Double,
    ): EffectiveTag =
        EffectiveTag(
            tagId = tagId,
            code = code,
            type = type,
            importance = importance,
        )

    private fun candidate(
        quoteId: Long,
        tagIdsByType: Map<TagType, Set<Long>>,
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

    private fun tag(
        id: Long,
        type: TagType,
        code: String,
    ): Tag =
        Tag(
            id = id,
            emotionRangeId = if (type == TagType.EMOTION) EMOTION_RANGE_ID else null,
            code = code,
            label = code,
            type = type,
            createdAt = CREATED_AT,
        )

    private companion object {
        const val USER_ID = 1L
        const val BOOK_ID = 10L
        const val EMOTION_RANGE_ID = 1L
        const val NEED_COMFORT_ID = 101L
        const val EMOTION_ANXIOUS_ID = 201L
        const val SITUATION_FAILURE_ID = 301L
        const val CONTEXT_RAIN_ID = 401L
        const val MOOD_WARM_ID = 501L
        const val MOOD_CALM_TONE_ID = 502L
        const val DELTA = 0.000001

        val CREATED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 13, 0, 0)
        val moodTagIdByCode: Map<String, Long> =
            mapOf(
                "MOOD_WARM" to MOOD_WARM_ID,
                "MOOD_CALM_TONE" to MOOD_CALM_TONE_ID,
            )
    }
}
