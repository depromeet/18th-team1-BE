package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.quotemetadata.dto.TagOption
import com.firstpenguin.app.domain.recommendation.model.RecommendationInput
import com.firstpenguin.app.domain.recommendation.model.TagCandidate
import com.firstpenguin.app.domain.recommendation.model.TagCandidatePriority
import com.firstpenguin.app.domain.recommendation.model.TagCandidateSource
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
            val priority = TagCandidatePriority.valueOf(requiredText("priority"))
            val tagOption = tagOptionByCode[tagCode] ?: return@runCatching null

            if (!isResolvable(tagCode, tagType, source, selectedCodes, tagOption)) {
                return@runCatching null
            }

            tagOption.toTagCandidate(input, source, priority)
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
        input: RecommendationInput,
        source: TagCandidateSource,
        priority: TagCandidatePriority,
    ): TagCandidate =
        TagCandidate(
            tagId = id,
            code = code,
            label = label,
            type = type,
            source = source,
            priority = priority.normalized(input, type, source),
        )

    private fun TagCandidatePriority.normalized(
        input: RecommendationInput,
        tagType: TagType,
        source: TagCandidateSource,
    ): TagCandidatePriority {
        val maxPriority =
            when {
                source == TagCandidateSource.DIARY_TEXT -> TagCandidatePriority.SECONDARY
                tagType == TagType.EMOTION -> TagCandidatePriority.SECONDARY
                tagType == TagType.NEED && input.needTag != null -> TagCandidatePriority.SECONDARY
                else -> TagCandidatePriority.PRIMARY
            }

        return atMost(maxPriority)
    }

    private fun TagCandidatePriority.atMost(maxPriority: TagCandidatePriority): TagCandidatePriority =
        if (rank() > maxPriority.rank()) maxPriority else this

    private fun TagCandidatePriority.rank(): Int =
        when (this) {
            TagCandidatePriority.BACKGROUND -> BACKGROUND_RANK
            TagCandidatePriority.SECONDARY -> SECONDARY_RANK
            TagCandidatePriority.PRIMARY -> PRIMARY_RANK
        }

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
        path(fieldName)
            .takeUnless { node -> node.isMissingNode || node.isNull }
            ?.asString()
            ?.takeIf { value -> value.isNotBlank() }
            ?: error("Missing required text field: $fieldName")

    private companion object {
        const val BACKGROUND_RANK = 1
        const val SECONDARY_RANK = 2
        const val PRIMARY_RANK = 3
    }
}
