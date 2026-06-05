package com.firstpenguin.app.domain.quotemetadata.service

import com.firstpenguin.app.domain.openai.dto.OpenAiBatchOutputItem
import com.firstpenguin.app.domain.openai.service.OpenAiBatchOutputReader
import com.firstpenguin.app.domain.quotemetadata.dto.ParsedBatchQuoteResult
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

@Component
class QuoteMetadataBatchOutputParser(
    private val objectMapper: ObjectMapper,
    private val openAiBatchOutputReader: OpenAiBatchOutputReader,
) {
    fun parseBatchOutputJsonl(jsonl: String): List<ParsedBatchQuoteResult> =
        openAiBatchOutputReader.readJsonl(jsonl).map { item -> parseBatchOutputItem(item) }

    private fun parseBatchOutputItem(item: OpenAiBatchOutputItem): ParsedBatchQuoteResult {
        val customId = item.customId
        val quoteId = quoteIdFromCustomId(customId)
        val errorMessage = item.errorMessage

        return if (errorMessage == null) {
            parseSuccessfulBatchOutput(item.outputText, customId, quoteId)
        } else {
            toErrorParsedBatchQuoteResult(customId, quoteId, errorMessage)
        }
    }

    private fun parseSuccessfulBatchOutput(
        outputText: String?,
        customId: String,
        quoteId: Long?,
    ): ParsedBatchQuoteResult =
        if (outputText.isNullOrBlank()) {
            toErrorParsedBatchQuoteResult(
                customId = customId,
                quoteId = quoteId,
                errorMessage = ErrorCode.QUOTE_METADATA_BATCH_OUTPUT_TEXT_NOT_FOUND.message,
            )
        } else {
            parseMetadataOutputText(customId, quoteId, outputText)
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
