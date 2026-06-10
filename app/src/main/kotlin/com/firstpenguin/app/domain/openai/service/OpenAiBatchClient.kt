package com.firstpenguin.app.domain.openai.service

import com.firstpenguin.app.domain.openai.dto.OpenAiBatchResponse
import com.firstpenguin.app.domain.openai.dto.OpenAiBatchStatusResponse
import com.firstpenguin.app.domain.openai.dto.OpenAiFileResponse
import com.firstpenguin.app.global.enums.BatchJobStatus
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.nio.file.Files
import java.time.Duration

private const val OPENAI_BASE_URL = "https://api.openai.com/v1"
private const val OPENAI_RESPONSES_ENDPOINT = "/v1/responses"
private const val OPENAI_CONNECT_TIMEOUT_SECONDS = 5L
private const val OPENAI_READ_TIMEOUT_MINUTES = 5L
private val OPENAI_CONNECT_TIMEOUT: Duration = Duration.ofSeconds(OPENAI_CONNECT_TIMEOUT_SECONDS)
private val OPENAI_READ_TIMEOUT: Duration = Duration.ofMinutes(OPENAI_READ_TIMEOUT_MINUTES)

@Component
class OpenAiBatchClient(
    @Value("\${openai.api-key:}") private val apiKey: String,
) {
    init {
        require(apiKey.isNotBlank()) { ErrorCode.OPENAI_API_KEY_REQUIRED.message }
    }

    private val restClient =
        RestClient
            .builder()
            .baseUrl(OPENAI_BASE_URL)
            .requestFactory(openAiRequestFactory())
            .defaultHeader("Authorization", "Bearer $apiKey")
            .build()

    fun uploadBatchInput(jsonl: String): OpenAiFileResponse {
        val tempFile = Files.createTempFile("openai-batch-", ".jsonl")
        Files.writeString(tempFile, jsonl)

        return try {
            val body =
                MultipartBodyBuilder()
                    .apply {
                        part("purpose", "batch")
                        part("file", FileSystemResource(tempFile))
                    }.build()

            restClient
                .post()
                .uri("/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body<OpenAiFileResponse>()
                ?: throw CustomException(ErrorCode.OPENAI_FILE_UPLOAD_FAILED)
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    fun createBatch(
        inputFileId: String,
        endpoint: String = OPENAI_RESPONSES_ENDPOINT,
    ): OpenAiBatchResponse {
        val response =
            restClient
                .post()
                .uri("/batches")
                .body(
                    mapOf(
                        "input_file_id" to inputFileId,
                        "endpoint" to endpoint,
                        "completion_window" to "24h",
                    ),
                ).retrieve()
                .body<Map<String, Any?>>()
                ?: throw CustomException(ErrorCode.OPENAI_BATCH_CREATE_FAILED)

        return OpenAiBatchResponse(
            id = response.getValue("id").toString(),
            status = BatchJobStatus.from(response.getValue("status").toString()),
        )
    }

    fun getStatus(batchId: String): OpenAiBatchStatusResponse {
        val response =
            restClient
                .get()
                .uri("/batches/{batchId}", batchId)
                .retrieve()
                .body<Map<String, Any?>>()
                ?: throw CustomException(ErrorCode.OPENAI_BATCH_STATUS_FETCH_FAILED)

        return OpenAiBatchStatusResponse(
            id = response.getValue("id").toString(),
            status = BatchJobStatus.from(response.getValue("status").toString()),
            outputFileId = response["output_file_id"] as? String,
            errorFileId = response["error_file_id"] as? String,
        )
    }

    fun fetchBatchOutputJsonl(outputFileId: String): String =
        restClient
            .get()
            .uri("/files/{outputFileId}/content", outputFileId)
            .retrieve()
            .body<String>()
            ?: ""

    private fun openAiRequestFactory(): SimpleClientHttpRequestFactory =
        SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(OPENAI_CONNECT_TIMEOUT)
            setReadTimeout(OPENAI_READ_TIMEOUT)
        }
}
