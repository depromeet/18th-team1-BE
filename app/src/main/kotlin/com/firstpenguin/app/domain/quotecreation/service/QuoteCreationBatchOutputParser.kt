package com.firstpenguin.app.domain.quotecreation.service

import com.firstpenguin.app.domain.openai.dto.OpenAiBatchOutputItem
import com.firstpenguin.app.domain.openai.service.OpenAiBatchOutputReader
import com.firstpenguin.app.domain.quotecreation.dto.ParsedQuoteCreationBatchResult
import com.firstpenguin.app.domain.quotecreation.model.QuoteCreationBatchResultType
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

@Component
class QuoteCreationBatchOutputParser(
    private val objectMapper: ObjectMapper,
    private val openAiBatchOutputReader: OpenAiBatchOutputReader,
) {
    fun parseBatchOutputJsonl(
        jsonl: String,
        resultType: QuoteCreationBatchResultType,
    ): List<ParsedQuoteCreationBatchResult> =
        openAiBatchOutputReader.readJsonl(jsonl).map { item -> parseBatchOutputItem(item, resultType) }

    private fun parseBatchOutputItem(
        item: OpenAiBatchOutputItem,
        resultType: QuoteCreationBatchResultType,
    ): ParsedQuoteCreationBatchResult {
        val customId = item.customId
        val bookId = bookIdFromCustomId(customId)
        val errorMessage = item.errorMessage

        return if (errorMessage == null) {
            parseSuccessfulBatchOutput(item.outputText, customId, bookId, resultType)
        } else {
            toErrorParsedQuoteCreationBatchResult(customId, bookId, errorMessage)
        }
    }

    private fun parseSuccessfulBatchOutput(
        outputText: String?,
        customId: String,
        bookId: Long?,
        resultType: QuoteCreationBatchResultType,
    ): ParsedQuoteCreationBatchResult =
        if (outputText.isNullOrBlank()) {
            toErrorParsedQuoteCreationBatchResult(
                customId = customId,
                bookId = bookId,
                errorMessage = ErrorCode.QUOTE_CREATION_BATCH_OUTPUT_TEXT_NOT_FOUND.message,
            )
        } else {
            parseQuoteOutputText(customId, bookId, outputText, resultType)
        }

    private fun parseQuoteOutputText(
        customId: String,
        bookId: Long?,
        outputText: String,
        resultType: QuoteCreationBatchResultType,
    ): ParsedQuoteCreationBatchResult =
        runCatching { objectMapper.readTree(outputText) }
            .fold(
                onSuccess = { outputNode ->
                    outputNode.toParsedQuoteCreationBatchResult(customId, bookId, resultType)
                },
                onFailure = { exception ->
                    toErrorParsedQuoteCreationBatchResult(customId, bookId, exception.message)
                },
            )
}

private fun JsonNode.toParsedQuoteCreationBatchResult(
    customId: String,
    fallbackBookId: Long?,
    resultType: QuoteCreationBatchResultType,
): ParsedQuoteCreationBatchResult =
    ParsedQuoteCreationBatchResult(
        customId = customId,
        bookId = longOrNull("bookId") ?: fallbackBookId,
        resultType = resultType,
        quoteContents = quoteContents(resultType),
        acceptedCandidateIds = acceptedCandidateIds(resultType),
        errorMessage = null,
    )

private fun toErrorParsedQuoteCreationBatchResult(
    customId: String,
    bookId: Long?,
    errorMessage: String?,
): ParsedQuoteCreationBatchResult =
    ParsedQuoteCreationBatchResult(
        customId = customId,
        bookId = bookId,
        resultType = null,
        quoteContents = emptyList(),
        acceptedCandidateIds = emptyList(),
        errorMessage = errorMessage,
    )

private fun JsonNode.quoteContents(resultType: QuoteCreationBatchResultType): List<String> =
    if (resultType == QuoteCreationBatchResultType.QUOTE_EXTRACTION) {
        path("quotes")
            .toList()
            .mapNotNull { quoteNode -> quoteNode.stringOrNull("content") }
    } else {
        emptyList()
    }

private fun JsonNode.acceptedCandidateIds(resultType: QuoteCreationBatchResultType): List<Long> =
    if (resultType == QuoteCreationBatchResultType.QUOTE_REVIEW) {
        longList("acceptedCandidateIds")
    } else {
        emptyList()
    }

private fun JsonNode.longList(fieldName: String): List<Long> =
    path(fieldName)
        .toList()
        .mapNotNull { node -> node.asString().toLongOrNull() }

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

private fun bookIdFromCustomId(customId: String): Long? =
    customId
        .removePrefix("book-")
        .toLongOrNull()
