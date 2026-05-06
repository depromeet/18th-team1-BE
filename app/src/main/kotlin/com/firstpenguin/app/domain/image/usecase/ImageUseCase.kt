package com.firstpenguin.app.domain.image.usecase

import com.firstpenguin.app.domain.image.dto.PresignedUrlRequest
import com.firstpenguin.app.domain.image.dto.PresignedUrlResponse
import com.firstpenguin.app.domain.image.service.ImageService
import org.springframework.stereotype.Component

@Component
class ImageUseCase(
    private val imageService: ImageService,
) {
    fun issuePresignedUrl(request: PresignedUrlRequest): PresignedUrlResponse {
        val (presignedUrl, imageId) = imageService.issue(request.type, request.contentType)
        return PresignedUrlResponse(presignedUrl, imageId)
    }
}
