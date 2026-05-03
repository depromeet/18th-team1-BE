package com.firstpenguin.app.domain.image.service

import com.firstpenguin.app.domain.image.model.ImageType

interface CloudStorageService {
    fun issuePresignedUrl(type: ImageType, contentType: String): Pair<String, String>
}
