package com.firstpenguin.app.domain.openai.service

import com.firstpenguin.app.domain.openai.dto.OpenAiResponsesRequest
import com.firstpenguin.app.domain.openai.dto.OpenAiTextResponse
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.body
import java.time.Duration

private const val OPENAI_BASE_URL = "https://api.openai.com/v1"
private const val OPENAI_RESPONSES_PATH = "/responses"
private const val OPENAI_CONNECT_TIMEOUT_SECONDS = 5L
private const val OPENAI_READ_TIMEOUT_SECONDS = 30L
private const val OUTPUT_TEXT_TYPE = "output_text"
private val OPENAI_CONNECT_TIMEOUT: Duration = Duration.ofSeconds(OPENAI_CONNECT_TIMEOUT_SECONDS)
private val OPENAI_READ_TIMEOUT: Duration = Duration.ofSeconds(OPENAI_READ_TIMEOUT_SECONDS)

@Component
class OpenAiResponsesClient(
    @Value("\${openai.api-key:}") private val apiKey: String,
) {
    init {
        if (apiKey.isBlank()) {
            throw CustomException(ErrorCode.OPENAI_API_KEY_REQUIRED)
        }
    }

    private val restClient =
        RestClient
            .builder()
            .baseUrl(OPENAI_BASE_URL)
            .requestFactory(openAiRequestFactory())
            .defaultHeader("Authorization", "Bearer $apiKey")
            .build()

    fun createTextResponse(request: OpenAiResponsesRequest): OpenAiTextResponse =
        runCatching {
            requestResponses(request).toTextResponse()
        }.getOrElse { exception ->
            throw exception.toResponsesException()
        }

    private fun requestResponses(request: OpenAiResponsesRequest): Map<String, Any?> =
        restClient
            .post()
            .uri(OPENAI_RESPONSES_PATH)
            .body(request)
            .retrieve()
            .body<Map<String, Any?>>()
            ?: throw CustomException(ErrorCode.OPENAI_RESPONSES_OUTPUT_TEXT_NOT_FOUND)

    private fun openAiRequestFactory(): SimpleClientHttpRequestFactory =
        SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(OPENAI_CONNECT_TIMEOUT)
            setReadTimeout(OPENAI_READ_TIMEOUT)
        }
}

private fun Map<String, Any?>.outputText(): String =
    directOutputText()
        ?: nestedOutputText()
        ?: throw CustomException(ErrorCode.OPENAI_RESPONSES_OUTPUT_TEXT_NOT_FOUND)

private fun Map<String, Any?>.toTextResponse(): OpenAiTextResponse =
    OpenAiTextResponse(
        outputText = outputText(),
        inputTokens = usageValue("input_tokens") ?: usageValue("prompt_tokens"),
        cachedTokens = inputTokenDetailsValue("cached_tokens"),
        outputTokens = usageValue("output_tokens") ?: usageValue("completion_tokens"),
    )

private fun Map<String, Any?>.directOutputText(): String? =
    this["output_text"]
        ?.toString()
        ?.takeIf { text -> text.isNotBlank() }

private fun Map<String, Any?>.nestedOutputText(): String? =
    outputItems()
        .flatMap { outputItem -> outputItem.contentItems() }
        .firstOrNull { content -> content["type"] == OUTPUT_TEXT_TYPE }
        ?.get("text")
        ?.toString()
        ?.takeIf { text -> text.isNotBlank() }

private fun Map<String, Any?>.outputItems(): List<Map<*, *>> =
    (this["output"] as? List<*>)
        .orEmpty()
        .mapNotNull { item -> item as? Map<*, *> }

private fun Map<*, *>.contentItems(): List<Map<*, *>> =
    (this["content"] as? List<*>)
        .orEmpty()
        .mapNotNull { item -> item as? Map<*, *> }

private fun Map<String, Any?>.usageValue(name: String): Long? =
    usage()
        ?.get(name)
        ?.toLongOrNull()

private fun Map<String, Any?>.inputTokenDetailsValue(name: String): Long? =
    (usage()?.get("input_tokens_details") as? Map<*, *>)
        ?.get(name)
        ?.toLongOrNull()
        ?: (usage()?.get("prompt_tokens_details") as? Map<*, *>)
            ?.get(name)
            ?.toLongOrNull()

private fun Map<String, Any?>.usage(): Map<*, *>? = this["usage"] as? Map<*, *>

private fun Any?.toLongOrNull(): Long? =
    when (this) {
        is Number -> {
            toLong()
        }

        is String -> {
            toLongOrNull()
        }

        else -> {
            null
        }
    }

private fun Throwable.toResponsesException(): Throwable =
    when (this) {
        is CustomException -> {
            this
        }

        is RestClientException -> {
            CustomException(ErrorCode.OPENAI_RESPONSES_REQUEST_FAILED)
                .also { exception -> exception.initCause(this) }
        }

        else -> {
            this
        }
    }
