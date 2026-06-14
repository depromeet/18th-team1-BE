package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.emotion.model.Tag
import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.IntentType
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.UserInputAnalysis
import com.firstpenguin.app.domain.recommendation.model.UserSemanticEmbedding
import com.firstpenguin.app.domain.recommendation.policy.MoodTagPolicy
import com.firstpenguin.app.domain.recommendation.repository.RecommendationCandidateProvider
import com.firstpenguin.app.global.enums.TagType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class RecommendationResultComposerTest {
    @Test
    fun `추천 결과는 top 10 quote와 main quote를 구성한다`() {
        val composer = composer()
        val candidates = (1L..12L).map { quoteId -> candidate(quoteId, roleTagId = quoteId) }

        val result =
            composer.compose(
                input = recommendationInput(),
                effectiveTags = effectiveTags,
                candidates = candidates,
                moodTagIdByCode = emptyMap(),
            )

        assertNotNull(result)
        assertEquals(10, result?.quotes?.size)
        assertEquals(result?.quotes?.first(), result?.mainQuote)
        assertEquals((1..10).toList(), result?.quotes?.map { quote -> quote.rank })
    }

    @Test
    fun `role tag가 한쪽으로 몰리면 뒤쪽 role 후보를 앞당긴다`() {
        val composer = composer()
        val candidates =
            (1L..8L)
                .map { quoteId -> candidate(quoteId, roleTagId = ROLE_TAG_ID) }
                .plus((9L..12L).map { quoteId -> candidate(quoteId, roleTagId = OTHER_ROLE_TAG_ID) })

        val result =
            composer.compose(
                input = recommendationInput(),
                effectiveTags = effectiveTags,
                candidates = candidates,
                moodTagIdByCode = emptyMap(),
            )
        val quoteIds = requireNotNull(result).quotes.map { quote -> quote.quoteId }

        assertTrue(quoteIds.indexOf(9L) < quoteIds.indexOf(4L))
        assertEquals(1L, quoteIds.first())
    }

    @Test
    fun `후보가 부족하면 fallback 후보를 보강해 top 10을 만든다`() {
        val provider =
            FakeRecommendationCandidateProvider(
                randomCandidates = (3L..10L).map { quoteId -> candidate(quoteId, roleTagId = quoteId) },
            )
        val composer = composer(provider)

        val result =
            composer.compose(
                input = recommendationInput(),
                effectiveTags = effectiveTags,
                candidates = listOf(candidate(1L, roleTagId = 1L), candidate(2L, roleTagId = 2L)),
                moodTagIdByCode = emptyMap(),
            )

        assertEquals(listOf("NEED", "EMOTION", "RELAXED", "RANDOM"), provider.calls)
        assertEquals((1L..10L).toList(), result?.quotes?.map { quote -> quote.quoteId })
    }

    @Test
    fun `canonicalIntent가 있으면 semanticScore를 최종 정렬에 반영한다`() {
        val semanticProvider =
            FakeRecommendationSemanticProvider(
                userEmbedding = UserSemanticEmbedding("불안한 마음을 진정시키고 싶다", listOf(0.1)),
                semanticScores = mapOf(2L to 1.0),
            )
        val composer = composer(semanticProvider = semanticProvider)

        val result =
            composer.compose(
                input = recommendationInput(canonicalIntent = "불안한 마음을 진정시키고 싶다"),
                effectiveTags = effectiveTags,
                candidates = listOf(candidate(1L, roleTagId = 1L), candidate(2L, roleTagId = 2L)),
                moodTagIdByCode = emptyMap(),
            )

        assertEquals(2L, result?.mainQuote?.quoteId)
        assertEquals(1.0, result?.mainQuote?.score?.semanticScore)
    }

    private class FakeRecommendationCandidateProvider(
        private val randomCandidates: List<RecommendationCandidate> = emptyList(),
    ) : RecommendationCandidateProvider {
        val calls = mutableListOf<String>()

        override fun findCandidates(
            effectiveTags: Collection<EffectiveTag>,
            limit: Int,
        ): List<RecommendationCandidate> {
            calls.add(effectiveTags.firstOrNull()?.type?.name ?: "EMPTY")

            return emptyList()
        }

        override fun findRelaxedCandidates(limit: Int): List<RecommendationCandidate> {
            calls.add("RELAXED")

            return emptyList()
        }

        override fun findRandomCandidates(limit: Int): List<RecommendationCandidate> {
            calls.add("RANDOM")

            return randomCandidates
        }

        override fun findCandidatesByQuoteIds(quoteIds: List<Long>): List<RecommendationCandidate> =
            quoteIds.map { quoteId -> candidate(quoteId, roleTagId = quoteId) }
    }

    private class FakeRecommendationSemanticProvider(
        private val userEmbedding: UserSemanticEmbedding? = null,
        private val semanticScores: Map<Long, Double> = emptyMap(),
        private val similarCandidates: List<RecommendationCandidate> = emptyList(),
    ) : RecommendationSemanticProvider {
        override fun prepare(input: RecommendationInput): UserSemanticEmbedding? = userEmbedding

        override fun findScores(
            userEmbedding: UserSemanticEmbedding?,
            quoteIds: Collection<Long>,
        ): Map<Long, Double> =
            if (userEmbedding == null) {
                emptyMap()
            } else {
                semanticScores.filterKeys { quoteId -> quoteId in quoteIds }
            }

        override fun findSimilarCandidates(
            userEmbedding: UserSemanticEmbedding?,
            excludedQuoteIds: Collection<Long>,
        ): List<RecommendationCandidate> =
            if (userEmbedding == null) {
                emptyList()
            } else {
                similarCandidates.filterNot { candidate -> candidate.quoteId in excludedQuoteIds }
            }
    }

    private companion object {
        const val USER_ID = 1L
        const val BOOK_ID = 10L
        const val EMOTION_RANGE_ID = 1L
        const val NEED_TAG_ID = 101L
        const val EMOTION_TAG_ID = 201L
        const val ROLE_TAG_ID = 301L
        const val OTHER_ROLE_TAG_ID = 302L

        val CREATED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 13, 0, 0)
        val effectiveTags =
            listOf(
                EffectiveTag(
                    tagId = NEED_TAG_ID,
                    code = "NEED_COMFORT",
                    type = TagType.NEED,
                ),
                EffectiveTag(
                    tagId = EMOTION_TAG_ID,
                    code = "EMOTION_ANXIOUS",
                    type = TagType.EMOTION,
                ),
            )

        fun composer(
            candidateProvider: RecommendationCandidateProvider = FakeRecommendationCandidateProvider(),
            semanticProvider: RecommendationSemanticProvider = FakeRecommendationSemanticProvider(),
        ): RecommendationResultComposer =
            RecommendationResultComposer(
                metadataScorer = MetadataScorer(MoodTagPolicy(), TypeScoreCalculator()),
                recommendationRanker = RecommendationRanker(),
                fallbackService = RecommendationFallbackService(candidateProvider),
                semanticProvider = semanticProvider,
            )

        fun recommendationInput(canonicalIntent: String? = null): RecommendationInput =
            RecommendationInput(
                userId = USER_ID,
                emotionRangeId = EMOTION_RANGE_ID,
                emotionTags = listOf(tag(EMOTION_TAG_ID, TagType.EMOTION, "EMOTION_ANXIOUS")),
                needTag = tag(NEED_TAG_ID, TagType.NEED, "NEED_COMFORT"),
                feelingText = null,
                diaryText = null,
                analysis =
                    UserInputAnalysis(
                        intentType = IntentType.EMOTION_NEED_BASED,
                        canonicalIntent = canonicalIntent,
                        tagCandidates = emptyList(),
                    ),
            )

        fun candidate(
            quoteId: Long,
            roleTagId: Long?,
        ): RecommendationCandidate =
            RecommendationCandidate(
                quoteId = quoteId,
                bookId = BOOK_ID,
                content = "content-$quoteId",
                title = "title",
                author = "author",
                roleTagId = roleTagId,
                tagIdsByType =
                    mapOf(
                        TagType.NEED to setOf(NEED_TAG_ID),
                        TagType.EMOTION to setOf(EMOTION_TAG_ID),
                    ),
            )

        fun tag(
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
    }
}
