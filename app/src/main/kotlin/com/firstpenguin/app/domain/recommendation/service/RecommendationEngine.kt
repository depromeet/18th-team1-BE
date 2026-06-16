package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.emotion.repository.TagRepository
import com.firstpenguin.app.domain.emotion.service.EmotionService
import com.firstpenguin.app.domain.recommendation.dto.RecommendationRequest
import com.firstpenguin.app.domain.recommendation.model.EffectiveTag
import com.firstpenguin.app.domain.recommendation.model.RecommendationCandidate
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.RecommendationResult
import com.firstpenguin.app.domain.recommendation.repository.RecommendationCandidateProvider
import com.firstpenguin.app.domain.recommendation.repository.RecommendationTagRarityRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private const val RECOMMENDATION_RESULT_QUOTE_COUNT = 10

@Service
class RecommendationEngine(
    private val emotionService: EmotionService,
    private val tagRepository: TagRepository,
    private val userInputAnalysisService: UserInputAnalysisService,
    private val effectiveTagBuilder: EffectiveTagBuilder,
    private val candidateProvider: RecommendationCandidateProvider,
    private val tagRarityRepository: RecommendationTagRarityRepository,
    private val resultComposer: RecommendationResultComposer,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun recommend(
        userId: Long,
        request: RecommendationRequest,
    ): RecommendationResult {
        var quoteCount = 0
        lateinit var result: RecommendationResult

        log.measureRecommendationStep("engine.total", { "userId=$userId quoteCount=$quoteCount" }) {
            val input = buildMeasuredInput(userId, request)
            val effectiveTags = buildMeasuredEffectiveTags(input)
            val candidates = findMeasuredCandidates(effectiveTags, userId)
            val moodTagIdByCode = findMeasuredMoodTags(userId)
            val tagRarityWeights = findMeasuredTagRarityWeights(userId)

            result =
                composeMeasuredResult(
                    input = input,
                    effectiveTags = effectiveTags,
                    candidates = candidates,
                    moodTagIdByCode = moodTagIdByCode,
                    tagRarityWeights = tagRarityWeights,
                )
            quoteCount = result.quotes.size
        }

        return result
    }

    private fun buildMeasuredInput(
        userId: Long,
        request: RecommendationRequest,
    ): RecommendationInput {
        val input =
            log.measureRecommendationStep("engine.buildInput", { "userId=$userId" }) {
                buildInput(userId, request)
            }
        var hasAnalysis = false
        lateinit var analyzedInput: RecommendationInput

        log.measureRecommendationStep("engine.userInputAnalysis", { "userId=$userId hasAnalysis=$hasAnalysis" }) {
            analyzedInput = input.withAnalysis()
            hasAnalysis = analyzedInput.analysis != null
        }

        return analyzedInput
    }

    private fun buildMeasuredEffectiveTags(input: RecommendationInput): List<EffectiveTag> {
        var effectiveTagCount = 0
        lateinit var effectiveTags: List<EffectiveTag>

        log.measureRecommendationStep("engine.effectiveTags", { "userId=${input.userId} count=$effectiveTagCount" }) {
            effectiveTags = effectiveTagBuilder.build(input)
            effectiveTagCount = effectiveTags.size
        }

        return effectiveTags
    }

    private fun findMeasuredCandidates(
        effectiveTags: List<EffectiveTag>,
        userId: Long,
    ): List<RecommendationCandidate> {
        var candidateCount = 0
        lateinit var candidates: List<RecommendationCandidate>

        log.measureRecommendationStep("engine.primaryCandidates", { "userId=$userId count=$candidateCount" }) {
            candidates = candidateProvider.findCandidates(effectiveTags)
            candidateCount = candidates.size
        }

        return candidates
    }

    private fun findMeasuredMoodTags(userId: Long): Map<String, Long> {
        var moodTagCount = 0
        lateinit var moodTagIdByCode: Map<String, Long>

        log.measureRecommendationStep("engine.moodTags", { "userId=$userId count=$moodTagCount" }) {
            moodTagIdByCode = tagRepository.getActiveMoodTagIdByCode()
            moodTagCount = moodTagIdByCode.size
        }

        return moodTagIdByCode
    }

    private fun findMeasuredTagRarityWeights(userId: Long): Map<Long, Double> {
        var tagRarityWeightCount = 0
        lateinit var tagRarityWeights: Map<Long, Double>

        log.measureRecommendationStep("engine.tagRarityWeights", { "userId=$userId count=$tagRarityWeightCount" }) {
            tagRarityWeights = tagRarityRepository.findMetadataTagRarityWeights()
            tagRarityWeightCount = tagRarityWeights.size
        }

        return tagRarityWeights
    }

    private fun composeMeasuredResult(
        input: RecommendationInput,
        effectiveTags: List<EffectiveTag>,
        candidates: List<RecommendationCandidate>,
        moodTagIdByCode: Map<String, Long>,
        tagRarityWeights: Map<Long, Double>,
    ): RecommendationResult {
        var quoteCount = 0
        lateinit var result: RecommendationResult

        log.measureRecommendationStep("engine.compose", { "userId=${input.userId} quoteCount=$quoteCount" }) {
            result =
                resultComposer.compose(
                    input = input,
                    effectiveTags = effectiveTags,
                    candidates = candidates,
                    moodTagIdByCode = moodTagIdByCode,
                    tagRarityWeights = tagRarityWeights,
                ) ?: notEnoughQuotes()
            quoteCount = result.quotes.size
        }

        return result
            .takeIf { recommendationResult -> recommendationResult.quotes.size >= RECOMMENDATION_RESULT_QUOTE_COUNT }
            ?: notEnoughQuotes()
    }

    private fun buildInput(
        userId: Long,
        request: RecommendationRequest,
    ): RecommendationInput {
        val selectedTagIds = request.emotionTagIds + listOfNotNull(request.needTagId)
        val (emotionTags, needTag) = emotionService.getEmotionTagsAndNeedTagByIds(selectedTagIds)

        return RecommendationInput(
            userId = userId,
            emotionRangeId = request.emotionRangeId,
            emotionTags = emotionTags,
            needTag = needTag,
            feelingText = request.feelingText.normalizedText(),
            diaryText = request.diaryText.normalizedText(),
            analysis = null,
        )
    }

    private fun RecommendationInput.withAnalysis(): RecommendationInput =
        copy(
            analysis = userInputAnalysisService.analyze(this),
        )
}

private fun String?.normalizedText(): String? = this?.trim()?.takeIf { text -> text.isNotEmpty() }

private fun notEnoughQuotes(): Nothing = throw CustomException(ErrorCode.NOT_ENOUGH_QUOTES)
