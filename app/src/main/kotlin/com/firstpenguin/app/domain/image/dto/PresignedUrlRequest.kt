package com.firstpenguin.app.domain.image.dto

import com.firstpenguin.app.domain.image.model.ImageType

data class PresignedUrlRequest(
    val type: ImageType,
    val contentType: String,
)
