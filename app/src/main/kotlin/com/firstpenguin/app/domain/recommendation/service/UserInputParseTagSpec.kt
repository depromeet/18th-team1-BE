package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.global.enums.TagType

internal data class UserInputParseTagSpec(
    val type: TagType,
    val fieldName: String,
    val description: String,
    val maxItems: Int,
)

internal val USER_INPUT_PARSE_TAG_SPECS =
    listOf(
        UserInputParseTagSpec(
            type = TagType.NEED,
            fieldName = "needTagCandidates",
            description = "텍스트에 명확히 드러난 필요/기대 후보",
            maxItems = 1,
        ),
        UserInputParseTagSpec(
            type = TagType.SITUATION,
            fieldName = "situationTagCandidates",
            description = "실제 삶의 문제, 사건, 관계, 주제가 직접 드러난 경우의 상황 후보",
            maxItems = 3,
        ),
        UserInputParseTagSpec(
            type = TagType.CONTEXT,
            fieldName = "contextTagCandidates",
            description = "실제 장소, 날씨, 시간, 활동, 장면이 직접 드러난 경우의 맥락 후보",
            maxItems = 3,
        ),
    )

internal val USER_INPUT_PARSE_TAG_TYPES: List<TagType> =
    USER_INPUT_PARSE_TAG_SPECS.map { spec -> spec.type }
