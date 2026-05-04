package com.firstpenguin.app.domain.image.service

import com.firstpenguin.app.domain.image.model.ImageType
import com.firstpenguin.app.global.config.GcsProperties
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.HttpMethod
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val PRESIGNED_URL_EXPIRATION_MINUTES = 15L

@Service
class GcsStorageService(
    private val gcsProperties: GcsProperties,
) : CloudStorageService {
    private val storage: Storage by lazy { StorageOptions.getDefaultInstance().service }

    override fun issuePresignedUrl(
        type: ImageType,
        contentType: String,
    ): Pair<String, String> {
        val extension = contentType.substringAfter("/")
        val objectKey = "${type.prefix}${UUID.randomUUID()}.$extension"
        val blobInfo =
            BlobInfo
                .newBuilder(gcsProperties.bucketName, objectKey)
                .setContentType(contentType)
                .build()

        val presignedUrl =
            storage
                .signUrl(
                    blobInfo,
                    PRESIGNED_URL_EXPIRATION_MINUTES,
                    TimeUnit.MINUTES,
                    Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                    Storage.SignUrlOption.withV4Signature(),
                ).toString()

        val publicUrl = "${gcsProperties.baseUrl}/$objectKey"
        return presignedUrl to publicUrl
    }
}
