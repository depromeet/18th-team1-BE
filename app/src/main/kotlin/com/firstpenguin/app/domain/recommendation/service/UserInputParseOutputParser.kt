package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.quotemetadata.dto.TagOption
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
        canonicalIntent = null,
        tagCandidates = tagCandidateMapper.map(tagCandidateNodes(), input, tagGroups),
    )

private fun JsonNode.tagCandidateNodes(): List<Pair<TagType, JsonNode>> =
    USER_INPUT_PARSE_TAG_SPECS.flatMap { spec ->
        path(spec.fieldName).toList().map { node -> spec.type to node }
    }
