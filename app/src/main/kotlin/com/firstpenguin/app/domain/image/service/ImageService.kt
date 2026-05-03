package com.firstpenguin.app.domain.image.service

import com.firstpenguin.app.domain.image.repository.ImageRepository
import com.firstpenguin.app.global.enums.ImageOwner
import org.springframework.stereotype.Service

@Service
class ImageService(
    private val imageRepository: ImageRepository,
) {
    fun findUrlById(id: Long): String? = imageRepository.findUrlById(id)

    fun findUrlsByOwnerIdAndOwnerType(ownerType: ImageOwner, ownerId: Long): List<String> =
        imageRepository.findUrlsByOwnerTypeAndOwnerId(ownerType, ownerId)
}
