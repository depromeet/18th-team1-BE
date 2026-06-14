package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.quotemetadata.dto.TagOption
import com.firstpenguin.app.domain.recommendation.model.IntentType
import com.firstpenguin.app.domain.recommendation.model.TagCandidatePriority
import com.firstpenguin.app.domain.recommendation.model.TagCandidateSource
import com.firstpenguin.app.global.enums.TagType

private const val CANONICAL_INTENT_MIN_LENGTH = 10
private const val CANONICAL_INTENT_MAX_LENGTH = 120
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
        "required" to requiredUserInputParseFields(),
        "properties" to userInputParseProperties(tagGroups),
    )

private fun requiredUserInputParseFields(): List<String> =
    listOf(
        "intentType",
        "canonicalIntent",
        "emotionTagCandidates",
        "needTagCandidates",
        "situationTagCandidates",
        "contextTagCandidates",
        "roleTagCandidates",
    )

private fun userInputParseProperties(tagGroups: Map<TagType, List<TagOption>>): Map<String, Any> =
    mapOf(
        "intentType" to
            enumSchema(
                values = IntentType.entries.map { type -> type.name },
                description = "사용자 입력의 추천 의도 유형",
            ),
        "canonicalIntent" to
            mapOf(
                "type" to "string",
                "description" to "사용자의 현재 마음과 원하는 도움을 요약한 한국어 한 문장",
                "minLength" to CANONICAL_INTENT_MIN_LENGTH,
                "maxLength" to CANONICAL_INTENT_MAX_LENGTH,
            ),
        "emotionTagCandidates" to
            tagCandidatesSchema(
                options = tagGroups.getValue(TagType.EMOTION),
                description = "텍스트에 명확히 드러난 사용자 감정 후보.",
                maxItems = EMOTION_TAG_MAX_ITEMS,
            ),
        "needTagCandidates" to
            tagCandidatesSchema(
                options = tagGroups.getValue(TagType.NEED),
                description = "텍스트에 명확히 드러난 필요/기대 후보. hasSelectedNeedTag=false이면 feelingText와 가장 일치하는 후보 1개를 반환한다.",
                maxItems = NEED_TAG_MAX_ITEMS,
            ),
        "situationTagCandidates" to
            tagCandidatesSchema(
                options = tagGroups.getValue(TagType.SITUATION),
                description = "실제 삶의 문제, 사건, 관계, 주제가 직접 드러난 경우의 상황 후보",
                maxItems = SUPPORTING_TAG_MAX_ITEMS,
            ),
        "contextTagCandidates" to
            tagCandidatesSchema(
                options = tagGroups.getValue(TagType.CONTEXT),
                description = "실제 장소, 날씨, 시간, 활동, 장면이 직접 드러난 경우의 맥락 후보",
                maxItems = SUPPORTING_TAG_MAX_ITEMS,
            ),
        "roleTagCandidates" to
            tagCandidatesSchema(
                options = tagGroups.getValue(TagType.ROLE),
                description = "사용자가 원하는 문장의 역할 후보. 명확한 경우에만 반환한다.",
                maxItems = ROLE_TAG_MAX_ITEMS,
            ),
    )

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
        "required" to listOf("tagCode", "source", "priority", "confidence"),
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
        "priority" to
            enumSchema(
                values = TagCandidatePriority.entries.map { priority -> priority.name },
                description = "같은 카테고리 안에서 추천 엔진에 전달할 우선순위",
            ),
        "confidence" to
            mapOf(
                "type" to "number",
                "description" to "텍스트 근거가 얼마나 명확한지 0.0부터 1.0 사이로 표현한다.",
                "minimum" to 0.0,
                "maximum" to 1.0,
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
