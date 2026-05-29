package com.firstpenguin.app.domain.batch.service

import com.firstpenguin.app.domain.batch.dto.OpenAiBatchResponse
import com.firstpenguin.app.domain.batch.dto.OpenAiFileResponse
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

    fun createBatch(inputFileId: String): OpenAiBatchResponse =
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
            .body<OpenAiBatchResponse>()!!
}
