package com.firstpenguin.app.domain.batch.service

import com.firstpenguin.app.domain.batch.dto.ParsedBatchQuoteResult
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

private const val SUCCESS_STATUS_CODE_MIN = 200
private const val SUCCESS_STATUS_CODE_MAX = 299
private const val OUTPUT_TEXT_TYPE = "output_text"

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
        val errorMessage = root.batchErrorMessage()

        return if (errorMessage == null) {
            parseSuccessfulBatchOutput(root, customId, quoteId)
        } else {
            toErrorParsedBatchQuoteResult(customId, quoteId, errorMessage)
        }
    }

    private fun parseSuccessfulBatchOutput(
        root: JsonNode,
        customId: String,
        quoteId: Long?,
    ): ParsedBatchQuoteResult {
        val outputText = root.outputText()

        return if (outputText.isNullOrBlank()) {
            toErrorParsedBatchQuoteResult(
                customId = customId,
                quoteId = quoteId,
                errorMessage = ErrorCode.QUOTE_METADATA_BATCH_OUTPUT_TEXT_NOT_FOUND.message,
            )
        } else {
            parseMetadataOutputText(customId, quoteId, outputText)
        }
    }

    private fun parseMetadataOutputText(
        customId: String,
        quoteId: Long?,
        outputText: String,
    ): ParsedBatchQuoteResult =
        runCatching { objectMapper.readTree(outputText) }
            .fold(
                onSuccess = { metadataNode -> metadataNode.toParsedBatchQuoteResult(customId, quoteId) },
                onFailure = { exception ->
                    toErrorParsedBatchQuoteResult(customId, quoteId, exception.message)
                },
            )
}

private fun JsonNode.toParsedBatchQuoteResult(
    customId: String,
    fallbackQuoteId: Long?,
): ParsedBatchQuoteResult =
    ParsedBatchQuoteResult(
        customId = customId,
        quoteId = longOrNull("quoteId") ?: fallbackQuoteId,
        roleTagCode = stringOrNull("roleTagCode"),
        emotionTagCodes = stringList("emotionTagCodes"),
        needTagCodes = stringList("needTagCodes"),
        situationTagCodes = stringList("situationTagCodes"),
        contextTagCodes = stringList("contextTagCodes"),
        moodTagCodes = stringList("moodTagCodes"),
        embeddingText = stringOrNull("embeddingText"),
        errorMessage = null,
    )

private fun JsonNode.batchErrorMessage(): String? {
    val errorNode = path("error")
    val directError = errorNode.takeUnless { node -> node.isMissingNode || node.isNull }

    return directError?.toString()
        ?: path("response")
            .takeIf { statusCode() !in SUCCESS_STATUS_CODE_MIN..SUCCESS_STATUS_CODE_MAX }
            ?.toString()
}

private fun JsonNode.statusCode(): Int =
    path("response")
        .path("status_code")
        .asInt()

private fun JsonNode.outputText(): String? =
    path("response")
        .path("body")
        .path("output")
        .flatMap { outputItem -> outputItem.path("content").toList() }
        .firstOrNull { content -> content.path("type").asString() == OUTPUT_TEXT_TYPE }
        ?.path("text")
        ?.asString()

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
        ?.asString()
        ?.toLongOrNull()

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
