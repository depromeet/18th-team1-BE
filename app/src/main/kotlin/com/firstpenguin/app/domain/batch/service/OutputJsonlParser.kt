package com.firstpenguin.app.domain.batch.service

import com.firstpenguin.app.domain.batch.dto.ParsedBatchQuoteResult
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

@Component
class OutputJsonlParser(
    private val objectMapper: ObjectMapper,
) {
    fun parseBatchOutputJsonl(jsonl: String): List<ParsedBatchQuoteResult> =
        jsonl
            .lineSequence()
            .filter { it.isNotBlank() }
            .map { parseBatchOutputJsonlLine(it) }
            .toList()

    private fun parseBatchOutputJsonlLine(line: String): ParsedBatchQuoteResult {
        val root = objectMapper.readTree(line)

        val customId = root.path("custom_id").asString()
        val quoteId = quoteIdFromCustomId(customId)
        val errorNode = root.path("error")

        if (!errorNode.isMissingNode && !errorNode.isNull) {
            return toErrorParsedBatchQuoteResult(
                customId = customId,
                quoteId = quoteId,
                errorMessage = errorNode.toString(),
            )
        }

        val statusCode =
            root
                .path("response")
                .path("status_code")
                .asInt()

        if (statusCode !in 200..299) {
            return toErrorParsedBatchQuoteResult(
                customId = customId,
                quoteId = quoteId,
                errorMessage = root.path("response").toString(),
            )
        }

        val output =
            root
                .path("response")
                .path("body")
                .path("output")
        val outputText =
            output
                .flatMap { outputItem ->
                    outputItem.path("content").toList()
                }.firstOrNull { content ->
                    content.path("type").asString() == "output_text"
                }?.path("text")
                ?.asString()

        if (outputText.isNullOrBlank()) {
            return toErrorParsedBatchQuoteResult(
                customId = customId,
                quoteId = quoteId,
                errorMessage = "output_text not found",
            )
        }

        val metadataNode =
            runCatching { objectMapper.readTree(outputText) }
                .getOrElse { exception ->
                    return toErrorParsedBatchQuoteResult(
                        customId = customId,
                        quoteId = quoteId,
                        errorMessage = exception.message,
                    )
                }

        return ParsedBatchQuoteResult(
            customId = customId,
            quoteId = metadataNode.longOrNull("quoteId") ?: quoteId,
            roleTagCode = metadataNode.stringOrNull("roleTagCode"),
            emotionTagCodes = metadataNode.stringList("emotionTagCodes"),
            needTagCodes = metadataNode.stringList("needTagCodes"),
            situationTagCodes = metadataNode.stringList("situationTagCodes"),
            contextTagCodes = metadataNode.stringList("contextTagCodes"),
            moodTagCodes = metadataNode.stringList("moodTagCodes"),
            embeddingText = metadataNode.stringOrNull("embeddingText"),
            errorMessage = null,
        )
    }

    private fun toErrorParsedBatchQuoteResult(
        customId: String,
        quoteId: Long?,
        errorMessage: String?,
    ): ParsedBatchQuoteResult =
        ParsedBatchQuoteResult(
            customId = customId,
            quoteId = quoteId,
            roleTagCode = null,
            emotionTagCodes = emptyList(),
            needTagCodes = emptyList(),
            situationTagCodes = emptyList(),
            contextTagCodes = emptyList(),
            moodTagCodes = emptyList(),
            embeddingText = null,
            errorMessage = errorMessage,
        )

    private fun JsonNode.longOrNull(fieldName: String): Long? =
        path(fieldName)
            .takeUnless { node -> node.isMissingNode || node.isNull }
            ?.asLong()

    private fun JsonNode.stringOrNull(fieldName: String): String? =
        path(fieldName)
            .takeUnless { node -> node.isMissingNode || node.isNull }
            ?.asString()
            ?.takeIf { value -> value.isNotBlank() }

    private fun JsonNode.stringList(fieldName: String): List<String> =
        path(fieldName)
            .toList()
            .mapNotNull { node -> node.asString().takeIf { value -> value.isNotBlank() } }

    private fun quoteIdFromCustomId(customId: String): Long? =
        customId
            .removePrefix("quote-")
            .toLongOrNull()
}
