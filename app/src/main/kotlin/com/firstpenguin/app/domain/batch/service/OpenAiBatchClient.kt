package com.firstpenguin.app.domain.batch.service

import com.firstpenguin.app.domain.batch.dto.ai.OpenAiBatchResponse
import com.firstpenguin.app.domain.batch.dto.ai.OpenAiBatchStatusResponse
import com.firstpenguin.app.domain.batch.dto.ai.OpenAiFileResponse
import com.firstpenguin.app.global.enums.BatchJobStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.nio.file.Files

@Component
class OpenAiBatchClient(
    @Value("\${openai.api-key:}") private val apiKey: String,
) {
    private val restClient =
        RestClient
            .builder()
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader("Authorization", "Bearer $apiKey")
            .build()

    fun uploadBatchInput(jsonl: String): OpenAiFileResponse {
        val tempFile = Files.createTempFile("quote-metadata-", ".jsonl")
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
                .body<OpenAiFileResponse>()!!
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    fun createBatch(inputFileId: String): OpenAiBatchResponse {
        val response =
            restClient
                .post()
                .uri("/batches")
                .body(
                    mapOf(
                        "input_file_id" to inputFileId,
                        "endpoint" to "/v1/responses",
                        "completion_window" to "24h",
                    ),
                ).retrieve()
                .body<Map<String, Any?>>()!!

        return response.toOpenAiBatchResponse()
    }

    fun getStatus(batchId: String): OpenAiBatchStatusResponse {
        val response =
            restClient
                .get()
                .uri("/batches/{batchId}", batchId)
                .retrieve()
                .body<Map<String, Any?>>()!!

        return OpenAiBatchStatusResponse(
            id = response.getValue("id").toString(),
            status = BatchJobStatus.from(response.getValue("status").toString()),
            outputFileId = response["output_file_id"] as? String,
            errorFileId = response["error_file_id"] as? String,
        )
    }

    private fun Map<String, Any?>.toOpenAiBatchResponse(): OpenAiBatchResponse =
        OpenAiBatchResponse(
            id = getValue("id").toString(),
            status = BatchJobStatus.from(getValue("status").toString()),
        )
}
