package com.firstpenguin.app.domain.batch.service

import com.firstpenguin.app.domain.batch.dto.TagOption
import com.firstpenguin.app.domain.quote.model.Quote
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
            ),
        "emotionTagCodes" to
            requiredCodeArraySchema(
                options = tagGroups.getValue(TagType.EMOTION),
            ),
        "needTagCodes" to
            requiredCodeArraySchema(
                options = tagGroups.getValue(TagType.NEED),
            ),
        "situationTagCodes" to
            optionalOneCodeArraySchema(
                options = tagGroups.getValue(TagType.SITUATION),
            ),
        "contextTagCodes" to
            optionalOneCodeArraySchema(
                options = tagGroups.getValue(TagType.CONTEXT),
            ),
        "moodTagCodes" to
            requiredOneCodeArraySchema(
                options = tagGroups.getValue(TagType.MOOD),
            ),
        "embeddingText" to
            mapOf(
                "type" to "string",
                "maxLength" to EMBEDDING_TEXT_MAX_LENGTH,
            ),
    )

private fun quoteIdSchema(quote: Quote): Map<String, Any> =
    mapOf(
        "type" to "integer",
        "enum" to listOf(quote.id),
    )

private fun codeSchema(options: List<TagOption>): Map<String, Any> =
    mapOf(
        "type" to "string",
        "enum" to options.map { option -> option.code },
    )

private fun requiredOneCodeArraySchema(options: List<TagOption>): Map<String, Any> =
    codeArraySchema(
        options = options,
        maxItems = STRICT_TAG_MAX_ITEMS,
        minItems = CORE_TAG_MIN_ITEMS,
    )

private fun requiredCodeArraySchema(options: List<TagOption>): Map<String, Any> =
    codeArraySchema(
        options = options,
        minItems = CORE_TAG_MIN_ITEMS,
    )

private fun optionalOneCodeArraySchema(options: List<TagOption>): Map<String, Any> =
    codeArraySchema(
        options = options,
        maxItems = STRICT_TAG_MAX_ITEMS,
    )

private fun codeArraySchema(
    options: List<TagOption>,
    maxItems: Int = TAG_MAX_ITEMS,
    minItems: Int = 0,
): Map<String, Any> =
    mapOf(
        "type" to "array",
        "minItems" to minItems,
        "maxItems" to maxItems,
        "items" to
            mapOf(
                "type" to "string",
                "enum" to options.map { option -> option.code },
            ),
    )
