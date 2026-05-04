package com.firstpenguin.app.domain.image.dto

data class PresignedUrlResponse(
    val presignedUrl: String,
    val imageId: Long,
)
