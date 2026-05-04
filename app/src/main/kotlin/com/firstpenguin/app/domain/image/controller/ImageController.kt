package com.firstpenguin.app.domain.image.controller

import com.firstpenguin.app.domain.image.dto.PresignedUrlRequest
import com.firstpenguin.app.domain.image.dto.PresignedUrlResponse
import com.firstpenguin.app.domain.image.service.ImageService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/images")
class ImageController(
    private val imageService: ImageService,
) {
    @PostMapping("/presigned-url")
    fun issuePresignedUrl(
        @RequestBody request: PresignedUrlRequest,
    ): ResponseEntity<PresignedUrlResponse> {
        val (presignedUrl, imageId) = imageService.issue(request.type, request.contentType)
        return ResponseEntity.ok(PresignedUrlResponse(presignedUrl, imageId))
    }
}
