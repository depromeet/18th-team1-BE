package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.quotemetadata.dto.TagOption
import com.firstpenguin.app.domain.recommendation.model.TagCandidateSource
import com.firstpenguin.app.global.enums.TagType

internal fun userInputParseSchema(tagGroups: Map<TagType, List<TagOption>>): Map<String, Any> =
    mapOf(
        "type" to "json_schema",
        "name" to "user_input_parse",
        "strict" to true,
        "schema" to userInputParseObjectSchema(tagGroups),
    )

private fun userInputParseObjectSchema(tagGroups: Map<TagType, List<TagOption>>): Map<String, Any> =
    mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "required" to requiredUserInputParseFields(tagGroups),
        "properties" to userInputParseProperties(tagGroups),
    )

private fun requiredUserInputParseFields(tagGroups: Map<TagType, List<TagOption>>): List<String> =
    tagSpecsOf(tagGroups)
        .map { spec -> spec.fieldName }

private fun userInputParseProperties(tagGroups: Map<TagType, List<TagOption>>): Map<String, Any> =
    tagSpecsOf(tagGroups)
        .associate { spec -> spec.fieldName to spec.tagCandidatesSchema(tagGroups.getValue(spec.type)) }

private fun tagSpecsOf(tagGroups: Map<TagType, List<TagOption>>): List<UserInputParseTagSpec> =
    USER_INPUT_PARSE_TAG_SPECS.filter { spec ->
        spec.type in tagGroups
    }

private fun tagCandidatesSchema(
    options: List<TagOption>,
    description: String,
    maxItems: Int,
): Map<String, Any> =
    mapOf(
        "type" to "array",
        "description" to description,
        "maxItems" to maxItems,
        "items" to tagCandidateSchema(options),
    )

private fun tagCandidateSchema(options: List<TagOption>): Map<String, Any> =
    mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "required" to listOf("tagCode", "source"),
        "properties" to tagCandidateProperties(options),
    )

private fun tagCandidateProperties(options: List<TagOption>): Map<String, Any> =
    mapOf(
        "tagCode" to
            enumSchema(
                values = options.map { option -> option.code },
                description = "반드시 이 카테고리에 허용된 tagCode 중 하나만 선택한다.",
            ),
        "source" to
            enumSchema(
                values = listOf(TagCandidateSource.FEELING_TEXT.name, TagCandidateSource.DIARY_TEXT.name),
                description = "태그 선택의 근거가 된 입력 텍스트",
            ),
    )

private fun enumSchema(
    values: List<String>,
    description: String,
): Map<String, Any> =
    mapOf(
        "type" to "string",
        "description" to description,
        "enum" to values.distinct(),
    )

private fun UserInputParseTagSpec.tagCandidatesSchema(options: List<TagOption>): Map<String, Any> =
    tagCandidatesSchema(
        options = options,
        description = description,
        maxItems = maxItems,
    )
