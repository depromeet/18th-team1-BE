package com.firstpenguin.app.domain.quotemetadata.service

import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.domain.quotemetadata.dto.TagOption
import com.firstpenguin.app.global.enums.TagType

private const val TAG_MAX_ITEMS = 2
private const val CORE_TAG_MIN_ITEMS = 1
private const val STRICT_TAG_MAX_ITEMS = 1
private const val EMBEDDING_TEXT_MAX_LENGTH = 80

internal fun quoteMetadataSchema(
    quote: Quote,
    tagGroups: Map<TagType, List<TagOption>>,
): Map<String, Any> =
    mapOf(
        "type" to "json_schema",
        "name" to "quote_metadata",
        "strict" to true,
        "schema" to quoteMetadataObjectSchema(quote, tagGroups),
    )

private fun quoteMetadataObjectSchema(
    quote: Quote,
    tagGroups: Map<TagType, List<TagOption>>,
): Map<String, Any> =
    mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "required" to requiredQuoteMetadataFields(),
        "properties" to quoteMetadataProperties(quote, tagGroups),
    )

private fun requiredQuoteMetadataFields(): List<String> =
    listOf(
        "quoteId",
        "roleTagCode",
        "emotionTagCodes",
        "needTagCodes",
        "situationTagCodes",
        "contextTagCodes",
        "moodTagCodes",
        "embeddingText",
    )

private fun quoteMetadataProperties(
    quote: Quote,
    tagGroups: Map<TagType, List<TagOption>>,
): Map<String, Any> =
    mapOf(
        "quoteId" to quoteIdSchema(quote),
        "roleTagCode" to
            codeSchema(
                options = tagGroups.getValue(TagType.ROLE),
                description = "이 인용구의 대표 반응 성격",
            ),
        "emotionTagCodes" to
            requiredCodeArraySchema(
                options = tagGroups.getValue(TagType.EMOTION),
                description = "이 인용구를 추천하기 좋은 사용자의 현재 감정 상태. 읽고 난 뒤 생길 감정은 제외한다.",
            ),
        "needTagCodes" to
            requiredCodeArraySchema(
                options = tagGroups.getValue(TagType.NEED),
                description = "이 인용구가 사용자에게 해주는 일이나 사용자가 원하는 도움",
            ),
        "situationTagCodes" to
            optionalOneCodeArraySchema(
                options = tagGroups.getValue(TagType.SITUATION),
                description = "원문에 직접 드러난 삶의 문제, 사건, 관계, 주제만 선택한다.",
            ),
        "contextTagCodes" to
            optionalOneCodeArraySchema(
                options = tagGroups.getValue(TagType.CONTEXT),
                description = "원문에 직접 드러난 실제 장소, 날씨, 시간, 활동, 장면만 선택한다.",
            ),
        "moodTagCodes" to
            requiredOneCodeArraySchema(
                options = tagGroups.getValue(TagType.MOOD),
                description = "이 인용구의 말투와 분위기. 사용자 감정이나 필요와 섞지 않는다.",
            ),
        "embeddingText" to
            mapOf(
                "type" to "string",
                "description" to "추천받을 사용자의 현재 마음과 원하는 도움을 담은 검색 의도 문장",
                "maxLength" to EMBEDDING_TEXT_MAX_LENGTH,
            ),
    )

private fun quoteIdSchema(quote: Quote): Map<String, Any> =
    mapOf(
        "type" to "integer",
        "enum" to listOf(quote.id),
    )

private fun codeSchema(
    options: List<TagOption>,
    description: String,
): Map<String, Any> =
    mapOf(
        "type" to "string",
        "description" to description,
        "enum" to options.map { option -> option.code },
    )

private fun requiredOneCodeArraySchema(
    options: List<TagOption>,
    description: String,
): Map<String, Any> =
    codeArraySchema(
        options = options,
        description = description,
        maxItems = STRICT_TAG_MAX_ITEMS,
        minItems = CORE_TAG_MIN_ITEMS,
    )

private fun requiredCodeArraySchema(
    options: List<TagOption>,
    description: String,
): Map<String, Any> =
    codeArraySchema(
        options = options,
        description = description,
        minItems = CORE_TAG_MIN_ITEMS,
    )

private fun optionalOneCodeArraySchema(
    options: List<TagOption>,
    description: String,
): Map<String, Any> =
    codeArraySchema(
        options = options,
        description = description,
        maxItems = STRICT_TAG_MAX_ITEMS,
    )

private fun codeArraySchema(
    options: List<TagOption>,
    description: String,
    maxItems: Int = TAG_MAX_ITEMS,
    minItems: Int = 0,
): Map<String, Any> =
    mapOf(
        "type" to "array",
        "description" to description,
        "minItems" to minItems,
        "maxItems" to maxItems,
        "items" to
            mapOf(
                "type" to "string",
                "enum" to options.map { option -> option.code },
            ),
    )
