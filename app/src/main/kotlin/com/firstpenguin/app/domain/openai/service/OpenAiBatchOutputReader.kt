package com.firstpenguin.app.domain.openai.service

import com.firstpenguin.app.domain.openai.dto.OpenAiBatchOutputItem
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

private const val SUCCESS_STATUS_CODE_MIN = 200
private const val SUCCESS_STATUS_CODE_MAX = 299
private const val OUTPUT_TEXT_TYPE = "output_text"

@Component
class OpenAiBatchOutputReader(
    private val objectMapper: ObjectMapper,
) {
    fun readJsonl(jsonl: String): List<OpenAiBatchOutputItem> =
        jsonl
            .lineSequence()
            .filter { line -> line.isNotBlank() }
            .map { line -> readLine(line) }
            .toList()

    private fun readLine(line: String): OpenAiBatchOutputItem {
        val root = objectMapper.readTree(line)
        return OpenAiBatchOutputItem(
            customId = root.path("custom_id").asString(),
            outputText = root.outputText(),
            errorMessage = root.batchErrorMessage(),
        )
    }
}

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
