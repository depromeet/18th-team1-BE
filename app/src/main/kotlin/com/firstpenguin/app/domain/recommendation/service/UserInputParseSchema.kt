package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.quotemetadata.dto.TagOption
import com.firstpenguin.app.domain.recommendation.model.TagCandidateSource
import com.firstpenguin.app.global.enums.TagType

private const val EMOTION_TAG_MAX_ITEMS = 2
private const val NEED_TAG_MAX_ITEMS = 1
private const val SUPPORTING_TAG_MAX_ITEMS = 3
private const val ROLE_TAG_MAX_ITEMS = 1

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
    tagTypesOf(tagGroups)
        .map { type -> type.fieldName() }

private fun userInputParseProperties(tagGroups: Map<TagType, List<TagOption>>): Map<String, Any> =
    tagTypesOf(tagGroups)
        .associate { type -> type.fieldName() to type.tagCandidatesSchema(tagGroups.getValue(type)) }

private fun tagTypesOf(tagGroups: Map<TagType, List<TagOption>>): List<TagType> =
    USER_INPUT_PARSE_TAG_TYPES.filter { tagType ->
        tagType in tagGroups
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

private fun TagType.fieldName(): String =
    when (this) {
        TagType.NEED -> "needTagCandidates"
        TagType.SITUATION -> "situationTagCandidates"
        TagType.CONTEXT -> "contextTagCandidates"
        TagType.ROLE -> "roleTagCandidates"
        TagType.EMOTION -> "emotionTagCandidates"
        else -> error("Unsupported tag type: $this")
    }

private fun TagType.tagCandidatesSchema(options: List<TagOption>): Map<String, Any> =
    when (this) {
        TagType.NEED -> {
            tagCandidatesSchema(
                options = options,
                description = "텍스트에 명확히 드러난 필요/기대 후보",
                maxItems = NEED_TAG_MAX_ITEMS,
            )
        }

        TagType.SITUATION -> {
            tagCandidatesSchema(
                options = options,
                description = "실제 삶의 문제, 사건, 관계, 주제가 직접 드러난 경우의 상황 후보",
                maxItems = SUPPORTING_TAG_MAX_ITEMS,
            )
        }

        TagType.CONTEXT -> {
            tagCandidatesSchema(
                options = options,
                description = "실제 장소, 날씨, 시간, 활동, 장면이 직접 드러난 경우의 맥락 후보",
                maxItems = SUPPORTING_TAG_MAX_ITEMS,
            )
        }

        TagType.ROLE -> {
            tagCandidatesSchema(
                options = options,
                description = "사용자가 원하는 문장의 역할 후보. 명확한 경우에만 반환한다.",
                maxItems = ROLE_TAG_MAX_ITEMS,
            )
        }

        TagType.EMOTION -> {
            tagCandidatesSchema(
                options = options,
                description = "텍스트에 명확히 드러난 사용자 감정 보조 후보",
                maxItems = EMOTION_TAG_MAX_ITEMS,
            )
        }

        else -> {
            error("Unsupported tag type: $this")
        }
    }
