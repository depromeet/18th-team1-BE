package com.firstpenguin.app.domain.openai.service

import com.firstpenguin.app.domain.embedding.model.OpenAiEmbeddingModelVersion
import com.firstpenguin.app.domain.openai.dto.OpenAiEmbeddingRequest
import com.firstpenguin.app.domain.openai.dto.OpenAiEmbeddingResponse
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
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
            executeWithRetry {
                requestEmbeddings(inputs, model).toEmbeddings()
            }
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

    private fun openAiRequestFactory(): SimpleClientHttpRequestFactory =
        SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(OPENAI_CONNECT_TIMEOUT)
            setReadTimeout(OPENAI_READ_TIMEOUT)
        }

    private fun <T> executeWithRetry(operation: () -> T): T {
        var delayMillis = INITIAL_RETRY_DELAY_MILLIS
        var lastException: Exception? = null

        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                return operation()
            } catch (exception: RestClientException) {
                lastException = exception

                if (!exception.isRetryable() || attempt == MAX_RETRY_ATTEMPTS - 1) {
                    throw exception
                }

                Thread.sleep(delayMillis)
                delayMillis =
                    (delayMillis * BACKOFF_MULTIPLIER)
                        .coerceAtMost(MAX_RETRY_DELAY_MILLIS)
            }
        }

        throw requireNotNull(lastException)
    }

    private companion object {
        const val MAX_RETRY_ATTEMPTS = 3
        const val INITIAL_RETRY_DELAY_MILLIS = 1_000L
        const val MAX_RETRY_DELAY_MILLIS = 8_000L
        const val BACKOFF_MULTIPLIER = 2
    }
}

private fun Throwable.toEmbeddingException(): Throwable =
    if (this is RestClientException) {
        CustomException(ErrorCode.QUOTE_EMBEDDING_OPENAI_REQUEST_FAILED).also { it.initCause(this) }
    } else {
        this
    }

private fun Throwable.isRetryable(): Boolean =
    when (this) {
        is HttpServerErrorException -> true
        is ResourceAccessException -> true
        is HttpClientErrorException.TooManyRequests -> true
        else -> false
    }
