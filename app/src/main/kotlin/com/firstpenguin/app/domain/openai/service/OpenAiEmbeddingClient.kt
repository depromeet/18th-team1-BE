package com.firstpenguin.app.domain.openai.service

import com.firstpenguin.app.domain.embedding.model.OpenAiEmbeddingModelVersion
import com.firstpenguin.app.domain.openai.dto.OpenAiEmbeddingRequest
import com.firstpenguin.app.domain.openai.dto.OpenAiEmbeddingResponse
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.body
import java.time.Duration

private const val OPENAI_BASE_URL = "https://api.openai.com/v1"
private const val OPENAI_EMBEDDINGS_PATH = "/embeddings"
private const val OPENAI_CONNECT_TIMEOUT_SECONDS = 5L
private const val OPENAI_READ_TIMEOUT_SECONDS = 30L
private val OPENAI_CONNECT_TIMEOUT: Duration = Duration.ofSeconds(OPENAI_CONNECT_TIMEOUT_SECONDS)
private val OPENAI_READ_TIMEOUT: Duration = Duration.ofSeconds(OPENAI_READ_TIMEOUT_SECONDS)

@Component
class OpenAiEmbeddingClient(
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

    fun createEmbedding(
        input: String,
        model: OpenAiEmbeddingModelVersion = OpenAiEmbeddingModelVersion.V1,
    ): List<Double> = createEmbeddings(listOf(input), model).first()

    fun createEmbeddings(
        inputs: List<String>,
        model: OpenAiEmbeddingModelVersion = OpenAiEmbeddingModelVersion.V1,
    ): List<List<Double>> =
        runCatching {
            validateInputs(inputs)
            requestEmbeddings(inputs, model).toEmbeddings()
        }.getOrElse { exception -> throw exception.toEmbeddingException() }

    private fun validateInputs(inputs: List<String>) {
        if (inputs.isEmpty() || inputs.any { input -> input.isBlank() }) {
            throw CustomException(ErrorCode.INVALID_QUOTE_EMBEDDING_INPUT)
        }
    }

    private fun requestEmbeddings(
        inputs: List<String>,
        model: OpenAiEmbeddingModelVersion,
    ): OpenAiEmbeddingResponse =
        restClient
            .post()
            .uri(OPENAI_EMBEDDINGS_PATH)
            .body(OpenAiEmbeddingRequest(model = model.model, input = inputs))
            .retrieve()
            .body<OpenAiEmbeddingResponse>()
            ?: throw CustomException(ErrorCode.QUOTE_EMBEDDING_RESPONSE_EMPTY)

    private fun openAiRequestFactory() =
        openAiClientHttpRequestFactory(
            connectTimeout = OPENAI_CONNECT_TIMEOUT,
            readTimeout = OPENAI_READ_TIMEOUT,
        )
}

private fun Throwable.toEmbeddingException(): Throwable =
    if (this is RestClientException) {
        CustomException(ErrorCode.QUOTE_EMBEDDING_OPENAI_REQUEST_FAILED).also { it.initCause(this) }
    } else {
        this
    }
