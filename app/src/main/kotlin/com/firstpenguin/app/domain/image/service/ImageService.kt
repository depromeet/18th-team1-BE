package com.firstpenguin.app.domain.image.service

import com.firstpenguin.app.domain.image.model.ImageType
import com.firstpenguin.app.domain.image.repository.ImageRepository
import org.springframework.stereotype.Service

private val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")

@Service
class ImageService(
    private val imageRepository: ImageRepository,
    private val cloudStorageService: CloudStorageService,
) {
    fun findUrlById(id: Long): String? = imageRepository.findUrlById(id)

    fun issue(type: ImageType, contentType: String): Pair<String, String> {
        require(contentType in ALLOWED_CONTENT_TYPES) {
            "허용되지 않는 contentType: $contentType"
        }
        return cloudStorageService.issuePresignedUrl(type, contentType)
    }
}
