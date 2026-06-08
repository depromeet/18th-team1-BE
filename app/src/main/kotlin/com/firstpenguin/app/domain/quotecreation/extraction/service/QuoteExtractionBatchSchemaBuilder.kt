package com.firstpenguin.app.domain.quotecreation.extraction.service

import com.firstpenguin.app.domain.book.model.Book

private const val RECOMMENDED_QUOTE_COUNT = 3
private const val MAX_QUOTE_LENGTH = 90

internal fun quoteExtractionSchema(book: Book): Map<String, Any> =
    mapOf(
        "type" to "json_schema",
        "name" to "quote_extraction",
        "strict" to true,
        "schema" to quoteExtractionSchemaBody(book),
    )

private fun quoteExtractionSchemaBody(book: Book): Map<String, Any> =
    mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "required" to listOf("bookId", "quotes"),
        "properties" to
            mapOf(
                "bookId" to mapOf("type" to "integer", "enum" to listOf(book.id)),
                "quotes" to quoteArraySchema(),
            ),
    )

private fun quoteArraySchema(): Map<String, Any> =
    mapOf(
        "type" to "array",
        "maxItems" to RECOMMENDED_QUOTE_COUNT,
        "items" to quoteItemSchema(),
    )

private fun quoteItemSchema(): Map<String, Any> =
    mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "required" to listOf("content"),
        "properties" to
            mapOf(
                "content" to mapOf("type" to "string", "maxLength" to MAX_QUOTE_LENGTH),
            ),
    )
