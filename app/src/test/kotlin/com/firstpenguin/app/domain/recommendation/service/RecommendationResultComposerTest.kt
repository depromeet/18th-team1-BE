package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.emotion.model.Tag
import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidateSource
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
    fun `role tag 다양화는 상위 점수 후보를 유지한 뒤 뒤쪽 후보에 적용한다`() {
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

        assertEquals((1L..3L).toList(), quoteIds.take(3))
        assertTrue(quoteIds.indexOf(9L) < quoteIds.indexOf(6L))
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

        assertEquals(listOf("EMOTION", "EMOTION_RANGE", "NEED", "RELAXED", "RANDOM"), provider.calls)
        assertEquals((1L..10L).toList(), result?.quotes?.map { quote -> quote.quoteId })
        assertEquals(RecommendationCandidateSource.PRIMARY, result?.quotes?.first()?.source)
        assertEquals(RecommendationCandidateSource.FALLBACK_RANDOM, result?.quotes?.last()?.source)
    }

    @Test
    fun `compose 기본 호출은 semantic embedding을 준비하지 않는다`() {
        val semanticProvider =
            FakeRecommendationSemanticProvider(
                userEmbedding = UserSemanticEmbedding("불안한 마음을 진정시키고 싶다", listOf(0.1)),
            )
        val composer = composer(semanticProvider = semanticProvider)

        composer.compose(
            input = recommendationInput(),
            effectiveTags = effectiveTags,
            candidates = (1L..10L).map { quoteId -> candidate(quoteId, roleTagId = quoteId) },
            moodTagIdByCode = emptyMap(),
        )

        assertEquals(0, semanticProvider.prepareCallCount)
    }

    @Test
    fun `canonicalIntent가 있으면 semanticScore를 최종 정렬에 반영한다`() {
        val userEmbedding = UserSemanticEmbedding("불안한 마음을 진정시키고 싶다", listOf(0.1))
        val semanticProvider =
            FakeRecommendationSemanticProvider(
                userEmbedding = userEmbedding,
                semanticScores = mapOf(2L to 1.0),
            )
        val composer = composer(semanticProvider = semanticProvider)
        val input = recommendationInput(canonicalIntent = "불안한 마음을 진정시키고 싶다")

        val result =
            composer.compose(
                input = input,
                effectiveTags = effectiveTags,
                candidates = listOf(candidate(1L, roleTagId = 1L), candidate(2L, roleTagId = 2L)),
                moodTagIdByCode = emptyMap(),
                userEmbedding = userEmbedding,
            )

        assertEquals(2L, result?.mainQuote?.quoteId)
        assertEquals(1.0, result?.mainQuote?.score?.semanticScore)
    }

    @Test
    fun `context situation metadata coverage가 낮으면 semantic score 비중을 높여 랭킹한다`() {
        val userEmbedding = UserSemanticEmbedding("좋아하는 사람을 떠올리며 설레는 마음을 오래 느끼고 싶다", listOf(0.1))
        val semanticProvider =
            FakeRecommendationSemanticProvider(
                userEmbedding = userEmbedding,
                semanticScores = mapOf(1L to 0.2, 2L to 0.8),
            )
        val composer = composer(semanticProvider = semanticProvider)
        val input = recommendationInput(canonicalIntent = "좋아하는 사람을 떠올리며 설레는 마음을 오래 느끼고 싶다")
        val situationEffectiveTags =
            effectiveTags +
                EffectiveTag(
                    tagId = SITUATION_TAG_ID,
                    code = "SITUATION_ROMANCE",
                    type = TagType.SITUATION,
                )
        val candidates =
            listOf(
                candidate(1L, roleTagId = 1L),
                candidate(2L, roleTagId = 2L, tagIdsByType = mapOf(TagType.EMOTION to setOf(EMOTION_TAG_ID))),
            ) + (3L..10L).map { quoteId -> candidate(quoteId, roleTagId = quoteId) }

        val result =
            composer.compose(
                input = input,
                effectiveTags = situationEffectiveTags,
                candidates = candidates,
                moodTagIdByCode = emptyMap(),
                userEmbedding = userEmbedding,
            )

        assertEquals(2L, result?.mainQuote?.quoteId)
    }

    @Test
    fun `semantic score가 없으면 metadataScore를 finalScore로 사용한다`() {
        val composer = composer()

        val result =
            composer.compose(
                input = recommendationInput(),
                effectiveTags = effectiveTags,
                candidates = (1L..10L).map { quoteId -> candidate(quoteId, roleTagId = quoteId) },
                moodTagIdByCode = emptyMap(),
            )
        val mainScore = requireNotNull(result).mainQuote.score

        assertEquals(mainScore.metadataScore, mainScore.finalScore)
    }

    @Test
    fun `감정 점수가 없는 후보는 감정 매칭 후보보다 뒤로 미룬다`() {
        val candidates =
            listOf(candidate(MISSING_EMOTION_QUOTE_ID, roleTagId = 1L, tagIdsByType = emptyMap())) +
                (2L..10L).map { quoteId -> candidate(quoteId, roleTagId = quoteId) }
        val userEmbedding = UserSemanticEmbedding("행복한 마음을 나누고 싶다", listOf(0.1))
        val semanticProvider =
            FakeRecommendationSemanticProvider(
                userEmbedding = userEmbedding,
                semanticScores = mapOf(MISSING_EMOTION_QUOTE_ID to LOW_SEMANTIC_SCORE),
            )
        val composer = composer(semanticProvider = semanticProvider)

        val result =
            composer.compose(
                input = recommendationInput(),
                effectiveTags = effectiveTags,
                candidates = candidates,
                moodTagIdByCode = emptyMap(),
                userEmbedding = userEmbedding,
            )
        val quoteIds = requireNotNull(result).quotes.map { quote -> quote.quoteId }

        assertEquals(MISSING_EMOTION_QUOTE_ID, quoteIds.last())
    }

    @Test
    fun `구체 태그가 있으면 semantic score가 높은 감정 미매칭 후보는 감정 후처리에서 뒤로 밀지 않는다`() {
        val candidates =
            listOf(candidate(MISSING_EMOTION_QUOTE_ID, roleTagId = 1L, tagIdsByType = emptyMap())) +
                (2L..10L).map { quoteId -> candidate(quoteId, roleTagId = quoteId) }
        val userEmbedding = UserSemanticEmbedding("퇴근길 실수 생각에 마음이 무겁다", listOf(0.1))
        val semanticProvider =
            FakeRecommendationSemanticProvider(
                userEmbedding = userEmbedding,
                semanticScores = mapOf(MISSING_EMOTION_QUOTE_ID to HIGH_SEMANTIC_SCORE),
            )
        val composer = composer(semanticProvider = semanticProvider)
        val input =
            recommendationInput(
                canonicalIntent = "퇴근길 실수 생각에 마음이 무겁다",
                feelingText = "퇴근길 실수 생각에 마음이 무겁다",
            )

        val result =
            composer.compose(
                input = input,
                effectiveTags = effectiveTags + effectiveTag(SITUATION_TAG_ID, "SITUATION_FAILURE", TagType.SITUATION),
                candidates = candidates,
                moodTagIdByCode = emptyMap(),
                userEmbedding = userEmbedding,
            )
        val quoteIds = requireNotNull(result).quotes.map { quote -> quote.quoteId }

        assertEquals(MISSING_EMOTION_QUOTE_ID, quoteIds.first())
    }

    @Test
    fun `짧은 감정 입력은 구체 태그가 없으면 semantic 후보를 섞지 않는다`() {
        val semanticCandidate =
            candidate(
                quoteId = HIGH_SEMANTIC_FALLBACK_QUOTE_ID,
                roleTagId = HIGH_SEMANTIC_FALLBACK_QUOTE_ID,
                tagIdsByType = emptyMap(),
            )
        val userEmbedding = UserSemanticEmbedding("현재 기쁜 마음을 이어가고 싶다", listOf(0.1))
        val semanticProvider =
            FakeRecommendationSemanticProvider(
                userEmbedding = userEmbedding,
                semanticScores = mapOf(HIGH_SEMANTIC_FALLBACK_QUOTE_ID to HIGH_SEMANTIC_SCORE),
                similarCandidates = listOf(semanticCandidate),
            )
        val composer = composer(semanticProvider = semanticProvider)
        val input =
            recommendationInput(
                canonicalIntent = "현재 기쁜 마음을 이어가고 싶다",
                feelingText = "행복해서!",
                emotionValue = HAPPY_EMOTION_VALUE,
            )

        val result =
            composer.compose(
                input = input,
                effectiveTags = effectiveTags,
                candidates = (1L..10L).map { quoteId -> candidate(quoteId, roleTagId = quoteId) },
                moodTagIdByCode = emptyMap(),
                userEmbedding = userEmbedding,
            )

        assertEquals(1L, result?.mainQuote?.quoteId)
        assertTrue(semanticProvider.similarCandidateLimits.isEmpty())
    }

    @Test
    fun `슬픈 짧은 입력도 구체 태그가 없으면 semantic 후보를 섞지 않는다`() {
        val semanticCandidate =
            candidate(
                quoteId = HIGH_SEMANTIC_FALLBACK_QUOTE_ID,
                roleTagId = HIGH_SEMANTIC_FALLBACK_QUOTE_ID,
                tagIdsByType = emptyMap(),
            )
        val userEmbedding = UserSemanticEmbedding("불안한 마음을 진정시키고 싶다", listOf(0.1))
        val semanticProvider =
            FakeRecommendationSemanticProvider(
                userEmbedding = userEmbedding,
                semanticScores = mapOf(HIGH_SEMANTIC_FALLBACK_QUOTE_ID to HIGH_SEMANTIC_SCORE),
                similarCandidates = listOf(semanticCandidate),
            )
        val composer = composer(semanticProvider = semanticProvider)
        val input =
            recommendationInput(
                canonicalIntent = "불안한 마음을 진정시키고 싶다",
                feelingText = "불안해",
            )

        composer.compose(
            input = input,
            effectiveTags = effectiveTags,
            candidates = (1L..10L).map { quoteId -> candidate(quoteId, roleTagId = quoteId) },
            moodTagIdByCode = emptyMap(),
            userEmbedding = userEmbedding,
        )

        assertTrue(semanticProvider.similarCandidateLimits.isEmpty())
    }

    @Test
    fun `긴 일반 감정 입력도 구체 태그가 없으면 semantic 후보를 섞지 않는다`() {
        val semanticCandidate =
            candidate(
                quoteId = HIGH_SEMANTIC_FALLBACK_QUOTE_ID,
                roleTagId = HIGH_SEMANTIC_FALLBACK_QUOTE_ID,
                tagIdsByType = mapOf(TagType.EMOTION to setOf(EMOTION_TAG_ID)),
            )
        val userEmbedding = UserSemanticEmbedding("지금의 좋은 에너지를 오래 간직하고 싶다", listOf(0.1))
        val semanticProvider =
            FakeRecommendationSemanticProvider(
                userEmbedding = userEmbedding,
                similarCandidates = listOf(semanticCandidate),
            )
        val composer = composer(semanticProvider = semanticProvider)
        val input =
            recommendationInput(
                canonicalIntent = "지금의 좋은 에너지를 오래 간직하고 싶다",
                feelingText = "오늘 기분이 너무 좋아서 이 에너지를 오래 간직하고 싶어",
                emotionValue = HAPPY_EMOTION_VALUE,
            )

        composer.compose(
            input = input,
            effectiveTags = effectiveTags,
            candidates = (1L..10L).map { quoteId -> candidate(quoteId, roleTagId = quoteId) },
            moodTagIdByCode = emptyMap(),
            userEmbedding = userEmbedding,
        )

        assertTrue(semanticProvider.similarCandidateLimits.isEmpty())
    }

    @Test
    fun `행복한 입력도 situation 태그가 있으면 semantic 후보를 섞는다`() {
        val semanticCandidate =
            candidate(
                quoteId = HIGH_SEMANTIC_FALLBACK_QUOTE_ID,
                roleTagId = HIGH_SEMANTIC_FALLBACK_QUOTE_ID,
                tagIdsByType = mapOf(TagType.SITUATION to setOf(SITUATION_TAG_ID)),
            )
        val userEmbedding = UserSemanticEmbedding("좋아하는 사람을 떠올리며 설레는 마음을 오래 느끼고 싶다", listOf(0.1))
        val semanticProvider =
            FakeRecommendationSemanticProvider(
                userEmbedding = userEmbedding,
                similarCandidates = listOf(semanticCandidate),
            )
        val composer = composer(semanticProvider = semanticProvider)
        val input =
            recommendationInput(
                canonicalIntent = "좋아하는 사람을 떠올리며 설레는 마음을 오래 느끼고 싶다",
                feelingText = "좋아하는 사람이 생각나서 설레고 행복해",
                emotionValue = HAPPY_EMOTION_VALUE,
            )
        val situationEffectiveTags =
            effectiveTags +
                EffectiveTag(
                    tagId = SITUATION_TAG_ID,
                    code = "SITUATION_ROMANCE",
                    type = TagType.SITUATION,
                )

        composer.compose(
            input = input,
            effectiveTags = situationEffectiveTags,
            candidates = (1L..10L).map { quoteId -> candidate(quoteId, roleTagId = quoteId) },
            moodTagIdByCode = emptyMap(),
            userEmbedding = userEmbedding,
        )

        assertEquals(listOf(SPECIFIC_SEMANTIC_SEED_CANDIDATE_LIMIT), semanticProvider.similarCandidateLimits)
    }

    @Test
    fun `행복한 입력에서는 metadata가 낮은 semantic 후보를 감정 후보 뒤로 미룬다`() {
        val candidates =
            listOf(candidate(MISSING_EMOTION_QUOTE_ID, roleTagId = 1L, tagIdsByType = emptyMap())) +
                (2L..10L).map { quoteId -> candidate(quoteId, roleTagId = quoteId) }
        val userEmbedding = UserSemanticEmbedding("현재 기쁜 마음을 이어가고 싶다", listOf(0.1))
        val semanticProvider =
            FakeRecommendationSemanticProvider(
                userEmbedding = userEmbedding,
                semanticScores = mapOf(MISSING_EMOTION_QUOTE_ID to HIGH_SEMANTIC_SCORE),
            )
        val composer = composer(semanticProvider = semanticProvider)
        val input =
            recommendationInput(
                canonicalIntent = "현재 기쁜 마음을 이어가고 싶다",
                feelingText = "행복해서!",
                emotionValue = HAPPY_EMOTION_VALUE,
            )

        val result =
            composer.compose(
                input = input,
                effectiveTags = effectiveTags,
                candidates = candidates,
                moodTagIdByCode = emptyMap(),
                userEmbedding = userEmbedding,
            )
        val quoteIds = requireNotNull(result).quotes.map { quote -> quote.quoteId }

        assertEquals(MISSING_EMOTION_QUOTE_ID, quoteIds.last())
    }

    @Test
    fun `metadataScore가 충분한 semantic fallback 후보는 top10에 유지한다`() {
        val semanticCandidate =
            candidate(
                quoteId = HIGH_SEMANTIC_FALLBACK_QUOTE_ID,
                roleTagId = HIGH_SEMANTIC_FALLBACK_QUOTE_ID,
                tagIdsByType =
                    mapOf(
                        TagType.NEED to setOf(NEED_TAG_ID),
                        TagType.EMOTION to setOf(EMOTION_TAG_ID),
                        TagType.SITUATION to setOf(SITUATION_TAG_ID),
                    ),
            )
        val userEmbedding = UserSemanticEmbedding("회사에서 한 실수가 계속 떠올라 마음이 무겁다", listOf(0.1))
        val semanticProvider =
            FakeRecommendationSemanticProvider(
                userEmbedding = userEmbedding,
                semanticScores = mapOf(HIGH_SEMANTIC_FALLBACK_QUOTE_ID to HIGH_SEMANTIC_SCORE),
                similarCandidates = listOf(semanticCandidate),
            )
        val composer = composer(semanticProvider = semanticProvider)
        val input =
            recommendationInput(
                canonicalIntent = "회사에서 한 실수가 계속 떠올라 마음이 무겁다",
                feelingText = "회사에서 한 실수가 계속 떠올라 마음이 무겁다",
            )

        val result =
            composer.compose(
                input = input,
                effectiveTags = effectiveTags + effectiveTag(SITUATION_TAG_ID, "SITUATION_FAILURE", TagType.SITUATION),
                candidates = (1L..9L).map { quoteId -> candidate(quoteId, roleTagId = quoteId) },
                moodTagIdByCode = emptyMap(),
                userEmbedding = userEmbedding,
            )

        val semanticQuote =
            requireNotNull(result).quotes.first { quote ->
                quote.quoteId == HIGH_SEMANTIC_FALLBACK_QUOTE_ID
            }

        assertEquals(RecommendationCandidateSource.FALLBACK_SEMANTIC, semanticQuote.source)
        assertEquals(listOf(SPECIFIC_SEMANTIC_SEED_CANDIDATE_LIMIT), semanticProvider.similarCandidateLimits)
    }

    @Test
    fun `metadataScore가 낮은 semantic fallback 후보는 strong semantic이어도 top10 뒤로 미룬다`() {
        val semanticCandidate =
            candidate(
                quoteId = HIGH_SEMANTIC_FALLBACK_QUOTE_ID,
                roleTagId = HIGH_SEMANTIC_FALLBACK_QUOTE_ID,
                tagIdsByType = emptyMap(),
            )
        val userEmbedding = UserSemanticEmbedding("회사에서 한 실수가 계속 떠올라 마음이 무겁다", listOf(0.1))
        val semanticProvider =
            FakeRecommendationSemanticProvider(
                userEmbedding = userEmbedding,
                semanticScores = mapOf(HIGH_SEMANTIC_FALLBACK_QUOTE_ID to HIGH_SEMANTIC_SCORE),
                similarCandidates = listOf(semanticCandidate),
            )
        val composer = composer(semanticProvider = semanticProvider)
        val input =
            recommendationInput(
                canonicalIntent = "회사에서 한 실수가 계속 떠올라 마음이 무겁다",
                feelingText = "회사에서 한 실수가 계속 떠올라 마음이 무겁다",
            )

        val result =
            composer.compose(
                input = input,
                effectiveTags = effectiveTags + effectiveTag(SITUATION_TAG_ID, "SITUATION_FAILURE", TagType.SITUATION),
                candidates = (1L..10L).map { quoteId -> candidate(quoteId, roleTagId = quoteId) },
                moodTagIdByCode = emptyMap(),
                userEmbedding = userEmbedding,
            )
        val quoteIds = requireNotNull(result).quotes.map { quote -> quote.quoteId }

        assertTrue(HIGH_SEMANTIC_FALLBACK_QUOTE_ID !in quoteIds)
        assertEquals(listOf(SPECIFIC_SEMANTIC_SEED_CANDIDATE_LIMIT), semanticProvider.similarCandidateLimits)
    }

    @Test
    fun `need fallback 후보는 감정 후보 뒤로 미룬다`() {
        val provider =
            FakeRecommendationCandidateProvider(
                needCandidates =
                    listOf(
                        candidate(
                            quoteId = HIGH_NEED_FALLBACK_QUOTE_ID,
                            roleTagId = HIGH_NEED_FALLBACK_QUOTE_ID,
                        ),
                    ),
            )
        val composer = composer(provider)
        val primaryCandidates =
            (1L..9L).map { quoteId ->
                candidate(
                    quoteId = quoteId,
                    roleTagId = quoteId,
                    tagIdsByType = mapOf(TagType.EMOTION to setOf(EMOTION_TAG_ID)),
                )
            }

        val result =
            composer.compose(
                input = recommendationInput(),
                effectiveTags = effectiveTags,
                candidates = primaryCandidates,
                moodTagIdByCode = emptyMap(),
            )
        val quotes = requireNotNull(result).quotes

        assertEquals((1L..9L).toList(), quotes.take(9).map { quote -> quote.quoteId })
        assertEquals(HIGH_NEED_FALLBACK_QUOTE_ID, quotes.last().quoteId)
        assertEquals(RecommendationCandidateSource.FALLBACK_NEED, quotes.last().source)
    }

    @Test
    fun `분석 대상 텍스트가 있으면 LLM 분석이 실패해도 분석 로그를 남긴다`() {
        val composer = composer()

        val result =
            composer.compose(
                input = recommendationInputWithoutAnalysis(diaryText = "행복해서!"),
                effectiveTags = effectiveTags,
                candidates = (1L..10L).map { quoteId -> candidate(quoteId, roleTagId = quoteId) },
                moodTagIdByCode = emptyMap(),
            )

        assertEquals("gpt-5-mini", result?.analysisLog?.llmModel)
        assertEquals(1, result?.analysisLog?.llmModelVersion)
        assertEquals(null, result?.analysisLog?.canonicalIntent)
        assertEquals(null, result?.analysisLog?.embeddingInputText)
    }

    @Test
    fun `LLM 분석이 실패해도 embedding이 성공하면 embedding input을 분석 로그에 남긴다`() {
        val composer = composer()

        val result =
            composer.compose(
                input = recommendationInputWithoutAnalysis(diaryText = "행복해서!"),
                effectiveTags = effectiveTags,
                candidates = (1L..10L).map { quoteId -> candidate(quoteId, roleTagId = quoteId) },
                moodTagIdByCode = emptyMap(),
                userEmbedding = UserSemanticEmbedding(FALLBACK_EMBEDDING_INPUT, listOf(0.1), EMBEDDING_ELAPSED_MS),
            )

        assertEquals(FALLBACK_EMBEDDING_INPUT, result?.analysisLog?.embeddingInputText)
        assertEquals(EMBEDDING_ELAPSED_MS, result?.analysisLog?.embeddingElapsedMs)
    }

    private class FakeRecommendationCandidateProvider(
        private val needCandidates: List<RecommendationCandidate> = emptyList(),
        private val randomCandidates: List<RecommendationCandidate> = emptyList(),
    ) : RecommendationCandidateProvider {
        val calls = mutableListOf<String>()

        override fun findCandidates(
            effectiveTags: Collection<EffectiveTag>,
            limit: Int,
        ): List<RecommendationCandidate> {
            val tagType = effectiveTags.firstOrNull()?.type
            calls.add(tagType?.name ?: "EMPTY")

            return if (tagType == TagType.NEED) {
                needCandidates
            } else {
                emptyList()
            }
        }

        override fun findCandidatesByEmotionRangeId(
            emotionRangeId: Long,
            limit: Int,
        ): List<RecommendationCandidate> {
            calls.add("EMOTION_RANGE")

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
        var prepareCallCount = 0
            private set

        override fun prepare(input: RecommendationInput): UserSemanticEmbedding? {
            prepareCallCount += 1

            return userEmbedding
        }

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
            limit: Int,
        ): List<RecommendationCandidate> =
            if (userEmbedding == null) {
                emptyList()
            } else {
                similarCandidateLimits.add(limit)
                similarCandidates.filterNot { candidate -> candidate.quoteId in excludedQuoteIds }
            }

        val similarCandidateLimits = mutableListOf<Int>()
    }

    private companion object {
        const val USER_ID = 1L
        const val BOOK_ID = 10L
        const val EMOTION_RANGE_ID = 1L
        const val EMOTION_VALUE = 1
        const val NEED_TAG_ID = 101L
        const val EMOTION_TAG_ID = 201L
        const val SITUATION_TAG_ID = 202L
        const val ROLE_TAG_ID = 301L
        const val OTHER_ROLE_TAG_ID = 302L
        const val MISSING_EMOTION_QUOTE_ID = 1L
        const val HIGH_NEED_FALLBACK_QUOTE_ID = 100L
        const val HIGH_SEMANTIC_FALLBACK_QUOTE_ID = 101L
        const val HIGH_SEMANTIC_SCORE = 1.0
        const val LOW_SEMANTIC_SCORE = 0.1
        const val HAPPY_EMOTION_VALUE = 8
        const val SPECIFIC_SEMANTIC_SEED_CANDIDATE_LIMIT = 30
        const val EMBEDDING_ELAPSED_MS = 123L
        const val FALLBACK_EMBEDDING_INPUT = "diaryText: 행복해서!"

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

        fun recommendationInput(
            canonicalIntent: String? = null,
            feelingText: String? = null,
            emotionValue: Int = EMOTION_VALUE,
        ): RecommendationInput =
            RecommendationInput(
                userId = USER_ID,
                emotionValue = emotionValue,
                emotionRangeId = EMOTION_RANGE_ID,
                emotionTags = listOf(tag(EMOTION_TAG_ID, TagType.EMOTION, "EMOTION_ANXIOUS")),
                needTag = tag(NEED_TAG_ID, TagType.NEED, "NEED_COMFORT"),
                feelingText = feelingText,
                diaryText = null,
                analysis =
                    UserInputAnalysis(
                        canonicalIntent = canonicalIntent,
                        tagCandidates = emptyList(),
                    ),
            )

        fun recommendationInputWithoutAnalysis(diaryText: String): RecommendationInput =
            RecommendationInput(
                userId = USER_ID,
                emotionValue = EMOTION_VALUE,
                emotionRangeId = EMOTION_RANGE_ID,
                emotionTags = listOf(tag(EMOTION_TAG_ID, TagType.EMOTION, "EMOTION_ANXIOUS")),
                needTag = tag(NEED_TAG_ID, TagType.NEED, "NEED_COMFORT"),
                feelingText = null,
                diaryText = diaryText,
                analysis = null,
            )

        fun candidate(
            quoteId: Long,
            roleTagId: Long?,
            tagIdsByType: Map<TagType, Set<Long>> =
                mapOf(
                    TagType.NEED to setOf(NEED_TAG_ID),
                    TagType.EMOTION to setOf(EMOTION_TAG_ID),
                ),
        ): RecommendationCandidate =
            RecommendationCandidate(
                quoteId = quoteId,
                bookId = BOOK_ID,
                content = "content-$quoteId",
                title = "title",
                author = "author",
                roleTagId = roleTagId,
                tagIdsByType = tagIdsByType,
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

        fun effectiveTag(
            tagId: Long,
            code: String,
            type: TagType,
        ): EffectiveTag =
            EffectiveTag(
                tagId = tagId,
                code = code,
                type = type,
            )
    }
}
