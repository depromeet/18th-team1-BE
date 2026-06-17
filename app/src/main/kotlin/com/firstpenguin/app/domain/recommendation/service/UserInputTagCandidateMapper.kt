package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.quotemetadata.dto.TagOption
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.TagCandidate
import com.firstpenguin.app.domain.recommendation.model.TagCandidatePriority
import com.firstpenguin.app.domain.recommendation.model.TagCandidateSource
import com.firstpenguin.app.domain.recommendation.policy.TagCandidatePriorityPolicy
import com.firstpenguin.app.global.enums.TagType
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode

@Component
class UserInputTagCandidateMapper {
    fun map(
        nodes: List<Pair<TagType, JsonNode>>,
        input: RecommendationInput,
        tagGroups: Map<TagType, List<TagOption>>,
    ): List<TagCandidate> {
        val selectedCodes = input.selectedTagCodes()
        val tagOptionByCode = tagGroups.toTagOptionByCode()

        val candidates =
            nodes
                .mapNotNull { (tagType, node) ->
                    node.toTagCandidate(tagType, input, selectedCodes, tagOptionByCode)
                }

        return candidates.distinctBy { candidate -> candidate.type to candidate.tagId }
    }

    private fun JsonNode.toTagCandidate(
        tagType: TagType,
        input: RecommendationInput,
        selectedCodes: Set<String>,
        tagOptionByCode: Map<String, TagOption>,
    ): TagCandidate? =
        runCatching {
            val tagCode = requiredText("tagCode")
            val source = TagCandidateSource.valueOf(requiredText("source"))
            val tagOption = tagOptionByCode[tagCode] ?: return@runCatching null

            if (!isResolvable(tagCode, tagType, source, selectedCodes, tagOption)) {
                return@runCatching null
            }

            tagOption.toTagCandidate(source, TagCandidatePriorityPolicy.resolve(input, tagType, source))
        }.getOrNull()

    private fun isResolvable(
        tagCode: String,
        tagType: TagType,
        source: TagCandidateSource,
        selectedCodes: Set<String>,
        tagOption: TagOption,
    ): Boolean =
        tagType in USER_INPUT_PARSE_TAG_TYPES &&
            source != TagCandidateSource.USER_SELECTED &&
            tagCode !in selectedCodes &&
            tagOption.type == tagType

    private fun TagOption.toTagCandidate(
        source: TagCandidateSource,
        priority: TagCandidatePriority,
    ): TagCandidate =
        TagCandidate(
            tagId = id,
            code = code,
            label = label,
            type = type,
            source = source,
            priority = priority,
        )

    private fun RecommendationInput.selectedTagCodes(): Set<String> =
        emotionTags
            .map { tag -> tag.code }
            .plus(listOfNotNull(needTag?.code))
            .toSet()

    private fun Map<TagType, List<TagOption>>.toTagOptionByCode(): Map<String, TagOption> =
        USER_INPUT_PARSE_TAG_TYPES
            .flatMap { type -> get(type).orEmpty() }
            .associateBy { option -> option.code }

    private fun JsonNode.requiredText(fieldName: String): String =
        stringOrNull(fieldName)
            ?.takeIf { value -> value.isNotBlank() }
            ?: error("Missing required text field: $fieldName")

    private fun JsonNode.stringOrNull(fieldName: String): String? =
        path(fieldName)
            .takeUnless { node -> node.isMissingNode || node.isNull }
            ?.asString()
}
