package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.quotemetadata.dto.TagOption
import com.firstpenguin.app.domain.recommendation.model.IntentType
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.UserInputAnalysis
import com.firstpenguin.app.global.enums.TagType
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

@Component
class UserInputParseOutputParser(
    private val objectMapper: ObjectMapper,
    private val tagCandidateMapper: UserInputTagCandidateMapper,
) {
    fun parse(
        outputText: String,
        input: RecommendationInput,
        tagGroups: Map<TagType, List<TagOption>>,
    ): UserInputAnalysis =
        objectMapper
            .readTree(outputText)
            .toUserInputAnalysis(input, tagGroups, tagCandidateMapper)
}

private fun JsonNode.toUserInputAnalysis(
    input: RecommendationInput,
    tagGroups: Map<TagType, List<TagOption>>,
    tagCandidateMapper: UserInputTagCandidateMapper,
): UserInputAnalysis =
    UserInputAnalysis(
        intentType = optionalIntentType(),
        canonicalIntent = null,
        tagCandidates = tagCandidateMapper.map(tagCandidateNodes(), input, tagGroups),
    )

private fun JsonNode.optionalIntentType(): IntentType =
    stringOrEmpty("intentType")
        .takeIf { value -> value.isNotBlank() }
        ?.let(IntentType::valueOf)
        ?: IntentType.EMOTION_NEED_BASED

private fun JsonNode.tagCandidateNodes(): List<Pair<TagType, JsonNode>> =
    listOf(
        TagType.EMOTION to "emotionTagCandidates",
        TagType.NEED to "needTagCandidates",
        TagType.SITUATION to "situationTagCandidates",
        TagType.CONTEXT to "contextTagCandidates",
        TagType.ROLE to "roleTagCandidates",
    ).flatMap { (type, fieldName) ->
        path(fieldName).toList().map { node -> type to node }
    }

private fun JsonNode.stringOrEmpty(fieldName: String): String =
    path(fieldName)
        .takeUnless { node -> node.isMissingNode || node.isNull }
        ?.asString()
        .orEmpty()
