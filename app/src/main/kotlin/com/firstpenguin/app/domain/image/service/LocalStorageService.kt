package com.firstpenguin.app.domain.image.service

import com.firstpenguin.app.domain.image.model.ImageType
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@ConditionalOnProperty(name = ["cloud.provider"], havingValue = "local", matchIfMissing = true)
class LocalStorageService : CloudStorageService {
    override fun issuePresignedUrl(
        type: ImageType,
        contentType: String,
    ): Pair<String, String> {
        val extension = contentType.substringAfter("/")
        val objectKey = "${type.prefix}${UUID.randomUUID()}.$extension"
        val fakePresignedUrl = "http://localhost:8080/mock-upload/$objectKey"
        val publicUrl = "http://localhost:8080/mock-images/$objectKey"
        return fakePresignedUrl to publicUrl
    }
}
