package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.emotion.model.EmotionRange
import com.firstpenguin.app.domain.emotion.model.Tag
import com.firstpenguin.app.domain.emotion.repository.TagRepository
import com.firstpenguin.app.domain.emotion.service.EmotionService
import com.firstpenguin.app.domain.recommendation.dto.RecommendationRequest
import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.IntentType
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.TagCandidate
import com.firstpenguin.app.domain.recommendation.model.TagCandidatePriority
import com.firstpenguin.app.domain.recommendation.model.TagCandidateSource
import com.firstpenguin.app.domain.recommendation.model.UserInputAnalysis
import com.firstpenguin.app.domain.recommendation.model.UserSemanticEmbedding
import com.firstpenguin.app.domain.recommendation.policy.MoodTagPolicy
import com.firstpenguin.app.domain.recommendation.repository.RecommendationCandidateProvider
import com.firstpenguin.app.domain.recommendation.repository.RecommendationTagRarityRepository
import com.firstpenguin.app.global.enums.EmotionRangeName
import com.firstpenguin.app.global.enums.TagType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

class RecommendationEngineTest {
    @Test
    fun `canonical 분석이 끝나면 후보 조회를 기다리지 않고 embedding을 시작한다`() {
        val calls = mutableListOf<String>()
        val semanticProvider = RecordingSemanticProvider(calls)
        val candidateProvider = RecordingCandidateProvider(calls)
        val engine = engine(calls, semanticProvider, candidateProvider)

        val result =
            engine.recommend(
                userId = USER_ID,
                request = recommendationRequest(),
            )

        assertEquals(RECOMMENDATION_RESULT_COUNT, result.quotes.size)
        assertEquals(listOf("analysis-start", "semantic-prepare", "candidate-prefetch"), calls.take(3))
        assertEquals(1, semanticProvider.prepareCallCount)
    }

    private class ImmediateUserInputAnalysisService(
        private val calls: MutableList<String>,
    ) : UserInputAnalysisService {
        override fun start(input: RecommendationInput): UserInputAnalysisTask {
            calls.add("analysis-start")
            val canonicalAnalysis =
                UserInputAnalysis(
                    intentType = IntentType.EMOTION_NEED_BASED,
                    canonicalIntent = CANONICAL_INTENT,
                    tagCandidates = emptyList(),
                )
            val tagAnalysis =
                canonicalAnalysis.copy(
                    tagCandidates =
                        listOf(
                            TagCandidate(
                                tagId = CONTEXT_TAG_ID,
                                code = "CONTEXT_RAIN",
                                label = "비",
                                type = TagType.CONTEXT,
                                source = TagCandidateSource.FEELING_TEXT,
                                priority = TagCandidatePriority.PRIMARY,
                            ),
                        ),
                )

            return UserInputAnalysisTask(
                canonicalAnalysis = CompletableFuture.completedFuture(canonicalAnalysis),
                analysis = CompletableFuture.completedFuture(tagAnalysis),
            )
        }
    }

    private class RecordingSemanticProvider(
        private val calls: MutableList<String>,
    ) : RecommendationSemanticProvider {
        var prepareCallCount: Int = 0

        override fun prepare(input: RecommendationInput): UserSemanticEmbedding? {
            prepareCallCount += 1
            calls.add("semantic-prepare")

            return UserSemanticEmbedding(CANONICAL_INTENT, listOf(0.1))
        }

        override fun findScores(
            userEmbedding: UserSemanticEmbedding?,
            quoteIds: Collection<Long>,
        ): Map<Long, Double> = emptyMap()

        override fun findSimilarCandidates(
            userEmbedding: UserSemanticEmbedding?,
            excludedQuoteIds: Collection<Long>,
        ): List<RecommendationCandidate> = emptyList()
    }

    private class RecordingCandidateProvider(
        private val calls: MutableList<String>,
    ) : RecommendationCandidateProvider {
        override fun findCandidates(
            effectiveTags: Collection<EffectiveTag>,
            limit: Int,
        ): List<RecommendationCandidate> {
            calls.add("candidate-prefetch")

            return candidates()
        }

        override fun findCandidatesByEmotionRangeId(
            emotionRangeId: Long,
            limit: Int,
        ): List<RecommendationCandidate> = emptyList()

        override fun findRelaxedCandidates(limit: Int): List<RecommendationCandidate> = emptyList()

        override fun findRandomCandidates(limit: Int): List<RecommendationCandidate> = emptyList()

        override fun findCandidatesByQuoteIds(quoteIds: List<Long>): List<RecommendationCandidate> = emptyList()
    }

    private companion object {
        const val USER_ID = 1L
        const val BOOK_ID = 10L
        const val EMOTION_RANGE_ID = 1L
        const val EMOTION_VALUE = 1
        const val EMOTION_TAG_ID = 101L
        const val CONTEXT_TAG_ID = 201L
        const val RECOMMENDATION_RESULT_COUNT = 10
        const val CANONICAL_INTENT = "비 오는 출근길에 불안한 마음을 차분히 다독이고 싶다"

        val CREATED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 17, 0, 0)

        fun engine(
            calls: MutableList<String>,
            semanticProvider: RecommendationSemanticProvider,
            candidateProvider: RecommendationCandidateProvider,
        ): RecommendationEngine =
            RecommendationEngine(
                emotionService = emotionService(),
                userInputAnalysisService = ImmediateUserInputAnalysisService(calls),
                effectiveTagBuilder = EffectiveTagBuilder(),
                prefetcher = prefetcher(candidateProvider),
                semanticProvider = semanticProvider,
                resultComposer = resultComposer(semanticProvider),
                analysisExecutor = java.util.concurrent.Executor { command -> command.run() },
            )

        fun emotionService(): EmotionService =
            Mockito.mock(EmotionService::class.java).apply {
                Mockito.`when`(getEmotionRangeByValue(EMOTION_VALUE))
                    .thenReturn(EmotionRange(EMOTION_RANGE_ID, EmotionRangeName.SAD, 1, 3))
                Mockito.`when`(getEmotionTagsAndNeedTagByIds(listOf(EMOTION_TAG_ID)))
                    .thenReturn(listOf(emotionTag()) to null)
            }

        fun prefetcher(candidateProvider: RecommendationCandidateProvider): RecommendationEnginePrefetcher {
            val tagRepository = Mockito.mock(TagRepository::class.java)
            val tagRarityRepository = Mockito.mock(RecommendationTagRarityRepository::class.java)
            Mockito.`when`(tagRepository.getActiveMoodTagIdByCode()).thenReturn(emptyMap())
            Mockito.`when`(tagRarityRepository.findMetadataTagRarityWeights()).thenReturn(emptyMap())

            return RecommendationEnginePrefetcher(
                candidateProvider = candidateProvider,
                tagRepository = tagRepository,
                tagRarityRepository = tagRarityRepository,
                executor = java.util.concurrent.Executor { command -> command.run() },
            )
        }

        fun resultComposer(semanticProvider: RecommendationSemanticProvider): RecommendationResultComposer =
            RecommendationResultComposer(
                metadataScorer = MetadataScorer(MoodTagPolicy(), TypeScoreCalculator()),
                recommendationRanker = RecommendationRanker(),
                fallbackService = RecommendationFallbackService(RecordingCandidateProvider(mutableListOf())),
                semanticProvider = semanticProvider,
            )

        fun recommendationRequest(): RecommendationRequest =
            RecommendationRequest(
                emotionValue = EMOTION_VALUE,
                emotionTagIds = listOf(EMOTION_TAG_ID),
                feelingText = "비 오는 출근길에 마음이 불안해",
            )

        fun candidates(): List<RecommendationCandidate> =
            (1L..RECOMMENDATION_RESULT_COUNT).map { quoteId ->
                RecommendationCandidate(
                    quoteId = quoteId,
                    bookId = BOOK_ID,
                    content = "content-$quoteId",
                    title = "title",
                    author = "author",
                    roleTagId = null,
                    tagIdsByType = mapOf(TagType.EMOTION to setOf(EMOTION_TAG_ID)),
                )
            }

        fun emotionTag(): Tag =
            Tag(
                id = EMOTION_TAG_ID,
                emotionRangeId = EMOTION_RANGE_ID,
                code = "EMOTION_ANXIOUS",
                label = "불안",
                type = TagType.EMOTION,
                createdAt = CREATED_AT,
            )
    }
}
