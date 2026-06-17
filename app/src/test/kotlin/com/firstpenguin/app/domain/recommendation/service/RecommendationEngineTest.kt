package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.emotion.model.EmotionRange
import com.firstpenguin.app.domain.emotion.model.Tag
import com.firstpenguin.app.domain.emotion.repository.TagRepository
import com.firstpenguin.app.domain.emotion.service.EmotionService
import com.firstpenguin.app.domain.recommendation.dto.RecommendationRequest
import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime
import java.util.Collections
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RecommendationEngineTest {
    @Test
    fun `canonical 분석이 끝나면 tag 분석 완료 전 embedding을 시작한다`() {
        val calls = Collections.synchronizedList(mutableListOf<String>())
        val analysisService = ControlledUserInputAnalysisService(calls, listOf(analyzedEmotionCandidate()))
        val semanticProvider = RecordingSemanticProvider(calls)
        val candidateProvider = RecordingCandidateProvider(calls)
        val engine = engine(analysisService, semanticProvider, candidateProvider)
        val executor = Executors.newSingleThreadExecutor()
        val resultFuture =
            CompletableFuture.supplyAsync(
                { engine.recommend(userId = USER_ID, request = recommendationRequest()) },
                executor,
            )

        try {
            analysisService.awaitStarted()
            candidateProvider.awaitPrefetchCount(1)
            assertEquals(setOf(TagType.EMOTION to EMOTION_TAG_ID), candidateProvider.prefetchedTagKeysAt(0))

            analysisService.completeCanonical()

            semanticProvider.awaitPrepared()
            assertEquals(1, semanticProvider.prepareCallCount)
            assertTrue(!analysisService.isAnalysisDone())
            assertEquals(1, candidateProvider.prefetchCallCount)

            analysisService.completeAnalysis()
            candidateProvider.awaitPrefetchCount(2)
            val result = resultFuture.get(1, TimeUnit.SECONDS)

            assertEquals(RECOMMENDATION_RESULT_COUNT, result.quotes.size)
            assertEquals(
                listOf("analysis-start", "candidate-prefetch", "semantic-prepare", "candidate-prefetch"),
                synchronized(calls) { calls.take(4) },
            )
            assertTrue((TagType.EMOTION to ANALYZED_EMOTION_TAG_ID) in candidateProvider.prefetchedTagKeysAt(1))
        } finally {
            analysisService.completeCanonical()
            analysisService.completeAnalysis()
            resultFuture.cancel(true)
            executor.shutdownNow()
        }
    }

    @Test
    fun `점수용 태그만 추가되면 후보 prefetch를 재사용한다`() {
        val calls = Collections.synchronizedList(mutableListOf<String>())
        val analysisService = ControlledUserInputAnalysisService(calls, listOf(contextCandidate()))
        val semanticProvider = RecordingSemanticProvider(calls)
        val candidateProvider = RecordingCandidateProvider(calls)
        val engine = engine(analysisService, semanticProvider, candidateProvider)
        val executor = Executors.newSingleThreadExecutor()
        val resultFuture =
            CompletableFuture.supplyAsync(
                { engine.recommend(userId = USER_ID, request = recommendationRequest()) },
                executor,
            )

        try {
            analysisService.awaitStarted()
            candidateProvider.awaitPrefetchCount(1)
            analysisService.completeCanonical()
            semanticProvider.awaitPrepared()
            analysisService.completeAnalysis()
            val result = resultFuture.get(1, TimeUnit.SECONDS)

            assertEquals(RECOMMENDATION_RESULT_COUNT, result.quotes.size)
            assertEquals(1, candidateProvider.prefetchCallCount)
            assertEquals(setOf(TagType.EMOTION to EMOTION_TAG_ID), candidateProvider.prefetchedTagKeysAt(0))
        } finally {
            analysisService.completeCanonical()
            analysisService.completeAnalysis()
            resultFuture.cancel(true)
            executor.shutdownNow()
        }
    }

    private class ControlledUserInputAnalysisService(
        private val calls: MutableList<String>,
        private val tagCandidates: List<TagCandidate>,
    ) : UserInputAnalysisService {
        private val started = CountDownLatch(1)
        private val canonicalAnalysisFuture = CompletableFuture<UserInputAnalysis?>()
        private val analysisFuture = CompletableFuture<UserInputAnalysis?>()

        override fun start(input: RecommendationInput): UserInputAnalysisTask {
            calls.add("analysis-start")
            started.countDown()

            return UserInputAnalysisTask(
                canonicalAnalysis = canonicalAnalysisFuture,
                analysis = analysisFuture,
            )
        }

        fun awaitStarted() {
            assertTrue(started.await(1, TimeUnit.SECONDS))
        }

        fun completeCanonical() {
            canonicalAnalysisFuture.complete(canonicalAnalysis())
        }

        fun completeAnalysis() {
            analysisFuture.complete(tagAnalysis())
        }

        fun isAnalysisDone(): Boolean = analysisFuture.isDone

        private fun canonicalAnalysis(): UserInputAnalysis =
            UserInputAnalysis(
                canonicalIntent = CANONICAL_INTENT,
                tagCandidates = emptyList(),
            )

        private fun tagAnalysis(): UserInputAnalysis =
            canonicalAnalysis()
                .copy(tagCandidates = tagCandidates)
    }

    private class RecordingSemanticProvider(
        private val calls: MutableList<String>,
    ) : RecommendationSemanticProvider {
        private val prepared = CountDownLatch(1)
        var prepareCallCount: Int = 0

        override fun prepare(input: RecommendationInput): UserSemanticEmbedding? {
            prepareCallCount += 1
            calls.add("semantic-prepare")
            prepared.countDown()

            return UserSemanticEmbedding(CANONICAL_INTENT, listOf(0.1))
        }

        fun awaitPrepared() {
            assertTrue(prepared.await(1, TimeUnit.SECONDS))
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
        private val firstPrefetch = CountDownLatch(1)
        private val secondPrefetch = CountDownLatch(1)
        private val prefetchedTagKeysByCall: MutableList<Set<Pair<TagType, Long>>> = mutableListOf()

        val prefetchCallCount: Int
            get() = synchronized(prefetchedTagKeysByCall) { prefetchedTagKeysByCall.size }

        override fun findCandidates(
            effectiveTags: Collection<EffectiveTag>,
            limit: Int,
        ): List<RecommendationCandidate> {
            calls.add("candidate-prefetch")
            val callCount = recordPrefetchedTags(effectiveTags)
            if (callCount == 1) {
                firstPrefetch.countDown()
            }
            if (callCount == 2) {
                secondPrefetch.countDown()
            }

            return candidates()
        }

        fun awaitPrefetchCount(count: Int) {
            val latch =
                if (count == 1) {
                    firstPrefetch
                } else {
                    secondPrefetch
                }
            assertTrue(latch.await(1, TimeUnit.SECONDS))
        }

        fun prefetchedTagKeysAt(index: Int): Set<Pair<TagType, Long>> =
            synchronized(
                prefetchedTagKeysByCall,
            ) { prefetchedTagKeysByCall[index] }

        private fun recordPrefetchedTags(effectiveTags: Collection<EffectiveTag>): Int =
            synchronized(prefetchedTagKeysByCall) {
                prefetchedTagKeysByCall.add(effectiveTags.map { tag -> tag.type to tag.tagId }.toSet())
                prefetchedTagKeysByCall.size
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
        const val ANALYZED_EMOTION_TAG_ID = 102L
        const val CONTEXT_TAG_ID = 201L
        const val RECOMMENDATION_RESULT_COUNT = 10
        const val CANONICAL_INTENT = "비 오는 출근길에 불안한 마음을 차분히 다독이고 싶다"

        val CREATED_AT: LocalDateTime = LocalDateTime.of(2026, 6, 17, 0, 0)

        fun engine(
            analysisService: UserInputAnalysisService,
            semanticProvider: RecommendationSemanticProvider,
            candidateProvider: RecommendationCandidateProvider,
        ): RecommendationEngine =
            RecommendationEngine(
                emotionService = emotionService(),
                userInputAnalysisService = analysisService,
                effectiveTagBuilder = EffectiveTagBuilder(),
                prefetcher = prefetcher(candidateProvider),
                semanticProvider = semanticProvider,
                resultComposer = resultComposer(semanticProvider),
                analysisExecutor = java.util.concurrent.Executor { command -> command.run() },
            )

        fun emotionService(): EmotionService =
            Mockito.mock(EmotionService::class.java).apply {
                Mockito
                    .`when`(getEmotionRangeByValue(EMOTION_VALUE))
                    .thenReturn(EmotionRange(EMOTION_RANGE_ID, EmotionRangeName.SAD, 1, 3))
                Mockito
                    .`when`(getEmotionTagsAndNeedTagByIds(listOf(EMOTION_TAG_ID)))
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

        fun analyzedEmotionCandidate(): TagCandidate =
            TagCandidate(
                tagId = ANALYZED_EMOTION_TAG_ID,
                code = "EMOTION_SAD",
                label = "슬픔",
                type = TagType.EMOTION,
                source = TagCandidateSource.FEELING_TEXT,
                priority = TagCandidatePriority.PRIMARY,
            )

        fun contextCandidate(): TagCandidate =
            TagCandidate(
                tagId = CONTEXT_TAG_ID,
                code = "CONTEXT_RAIN",
                label = "비",
                type = TagType.CONTEXT,
                source = TagCandidateSource.FEELING_TEXT,
                priority = TagCandidatePriority.PRIMARY,
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
