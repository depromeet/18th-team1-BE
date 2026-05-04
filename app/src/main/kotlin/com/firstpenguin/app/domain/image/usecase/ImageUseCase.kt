package com.firstpenguin.app.domain.image.usecase

import com.firstpenguin.app.domain.image.dto.PresignedUrlResponse
import com.firstpenguin.app.domain.image.model.ImageType
import com.firstpenguin.app.domain.image.service.ImageService
import org.springframework.stereotype.Component

@Component
class ImageUseCase(
    private val imageService: ImageService,
) {
    fun issuePresignedUrl(
        type: ImageType,
        contentType: String,
    ): PresignedUrlResponse {
        val (presignedUrl, imageId) = imageService.issue(type, contentType)
        return PresignedUrlResponse(presignedUrl, imageId)
    }
}
