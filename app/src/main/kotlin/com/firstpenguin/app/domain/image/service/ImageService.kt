package com.firstpenguin.app.domain.image.service

import com.firstpenguin.app.domain.image.repository.ImageRepository
import org.springframework.stereotype.Service

@Service
class ImageService(
    private val imageRepository: ImageRepository,
) {
    fun findUrlById(id: Long): String? = imageRepository.findUrlById(id)
}
