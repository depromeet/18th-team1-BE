package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.RankedRecommendationQuote
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.RecommendationScoreBreakdown
import com.firstpenguin.app.domain.recommendation.repository.RecommendationCandidateProvider
import com.firstpenguin.app.global.enums.TagType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private typealias Candidate = RecommendationCandidate

class RecommendationFallbackServiceTest {
    @Test
    fun `후보가 충분하고 top score가 낮지 않으면 fallback 조회하지 않는다`() {
        val provider = FakeRecommendationCandidateProvider()
        val service = RecommendationFallbackService(provider)
        val existingCandidates = (1L..10L).map(::candidate)

        val result =
            service.supplementCandidates(
                effectiveTags = effectiveTags,
                existingCandidates = existingCandidates,
                rankedQuotes = listOf(rankedQuote(existingCandidates.first(), finalScore = HIGH_SCORE)),
            )

        assertEquals(existingCandidates, result)
        assertTrue(provider.calls.isEmpty())
    }

    @Test
    fun `후보가 부족하면 need emotion relaxed random 순서로 보강한다`() {
        val provider =
            FakeRecommendationCandidateProvider(
                needCandidates = listOf(candidate(1L), candidate(2L)),
                emotionCandidates = listOf(candidate(3L)),
                relaxedCandidates = listOf(candidate(4L)),
                randomCandidates = (5L..10L).map(::candidate),
            )
        val service = RecommendationFallbackService(provider)

        val result =
            service.supplementCandidates(
                effectiveTags = effectiveTags,
                existingCandidates = listOf(candidate(1L)),
            )

        assertEquals(listOf("NEED", "EMOTION", "RELAXED", "RANDOM"), provider.calls)
        assertEquals((1L..10L).toList(), result.map { candidate -> candidate.quoteId })
    }

    @Test
    fun `semantic 후보는 emotion 다음 relaxed 이전에 보강한다`() {
        val events = mutableListOf<String>()
        val provider =
            FakeRecommendationCandidateProvider(
                randomCandidates = (5L..10L).map(::candidate),
                events = events,
            )
        val service = RecommendationFallbackService(provider)

        val result =
            service.supplementCandidates(
                effectiveTags = effectiveTags,
                existingCandidates = listOf(candidate(1L)),
                semanticCandidates = {
                    events.add("SEMANTIC")
                    listOf(candidate(2L), candidate(3L), candidate(4L))
                },
            )

        assertEquals(listOf("NEED", "EMOTION", "SEMANTIC", "RELAXED", "RANDOM"), events)
        assertEquals((1L..10L).toList(), result.map { candidate -> candidate.quoteId })
    }

    @Test
    fun `need emotion fallback은 해당 타입 effectiveTag만 전달한다`() {
        val provider = FakeRecommendationCandidateProvider(randomCandidates = (1L..10L).map(::candidate))
        val service = RecommendationFallbackService(provider)

        service.supplementCandidates(
            effectiveTags = effectiveTags,
            existingCandidates = emptyList(),
        )

        assertEquals(listOf(TagType.NEED), provider.capturedTypes[0])
        assertEquals(listOf(TagType.EMOTION), provider.capturedTypes[1])
    }

    @Test
    fun `top score가 낮으면 후보가 충분해도 모든 fallback 단계를 검토한다`() {
        val provider =
            FakeRecommendationCandidateProvider(
                needCandidates = listOf(candidate(11L)),
                emotionCandidates = listOf(candidate(12L)),
                relaxedCandidates = listOf(candidate(13L)),
                randomCandidates = listOf(candidate(14L)),
            )
        val service = RecommendationFallbackService(provider)
        val existingCandidates = (1L..10L).map(::candidate)

        val result =
            service.supplementCandidates(
                effectiveTags = effectiveTags,
                existingCandidates = existingCandidates,
                rankedQuotes = listOf(rankedQuote(existingCandidates.first(), finalScore = LOW_SCORE)),
            )

        assertEquals(listOf("NEED", "EMOTION", "RELAXED", "RANDOM"), provider.calls)
        assertEquals((1L..14L).toList(), result.map { candidate -> candidate.quoteId })
    }

    private class FakeRecommendationCandidateProvider(
        private val needCandidates: List<RecommendationCandidate> = emptyList(),
        private val emotionCandidates: List<RecommendationCandidate> = emptyList(),
        private val relaxedCandidates: List<RecommendationCandidate> = emptyList(),
        private val randomCandidates: List<RecommendationCandidate> = emptyList(),
        private val events: MutableList<String>? = null,
    ) : RecommendationCandidateProvider {
        val calls = mutableListOf<String>()
        val capturedTypes = mutableListOf<List<TagType>>()

        override fun findCandidates(
            effectiveTags: Collection<EffectiveTag>,
            limit: Int,
        ): List<RecommendationCandidate> {
            val tagTypes = effectiveTags.map { tag -> tag.type }.distinct()
            capturedTypes.add(tagTypes)

            return when (tagTypes.singleOrNull()) {
                TagType.NEED -> {
                    record("NEED")
                    needCandidates
                }

                TagType.EMOTION -> {
                    record("EMOTION")
                    emotionCandidates
                }

                else -> {
                    emptyList()
                }
            }
        }

        override fun findRelaxedCandidates(limit: Int): List<RecommendationCandidate> {
            record("RELAXED")

            return relaxedCandidates
        }

        override fun findRandomCandidates(limit: Int): List<RecommendationCandidate> {
            record("RANDOM")

            return randomCandidates
        }

        override fun findCandidatesByQuoteIds(quoteIds: List<Long>): List<Candidate> = quoteIds.map(::candidate)

        private fun record(call: String) {
            calls.add(call)
            events?.add(call)
        }
    }

    private companion object {
        const val BOOK_ID = 100L
        const val NEED_TAG_ID = 101L
        const val EMOTION_TAG_ID = 201L
        const val CONTEXT_TAG_ID = 301L
        const val HIGH_SCORE = 0.8
        const val LOW_SCORE = 0.1

        val effectiveTags =
            listOf(
                effectiveTag(NEED_TAG_ID, TagType.NEED),
                effectiveTag(EMOTION_TAG_ID, TagType.EMOTION),
                effectiveTag(CONTEXT_TAG_ID, TagType.CONTEXT),
            )

        fun effectiveTag(
            tagId: Long,
            tagType: TagType,
        ): EffectiveTag =
            EffectiveTag(
                tagId = tagId,
                code = "${tagType.name}_$tagId",
                type = tagType,
            )

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

        fun rankedQuote(
            candidate: RecommendationCandidate,
            finalScore: Double,
        ): RankedRecommendationQuote =
            RankedRecommendationQuote(
                rank = 1,
                candidate = candidate,
                score =
                    RecommendationScoreBreakdown(
                        needScore = 0.0,
                        emotionScore = 0.0,
                        contextScore = 0.0,
                        situationScore = 0.0,
                        moodScore = 0.0,
                        metadataScore = finalScore,
                        semanticScore = 0.0,
                        finalScore = finalScore,
                    ),
            )
    }
}
