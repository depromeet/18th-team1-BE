package com.firstpenguin.app.domain.image.service

import com.firstpenguin.app.domain.image.helper.SharedCalendarImageHelper
import com.firstpenguin.app.domain.image.helper.SharedQuoteImageHelper
import com.firstpenguin.app.domain.image.model.ImageType
import com.firstpenguin.app.domain.image.repository.ImageRepository
import com.firstpenguin.app.global.enums.ImageOwner
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service
import java.time.LocalDate

private val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")

@Service
class ImageService(
    private val imageRepository: ImageRepository,
    private val cloudStorageService: CloudStorageService,
) {
    fun findUrlById(id: Long): String? = imageRepository.findUrlById(id)

    fun validateExists(id: Long) {
        if (!imageRepository.existsById(id)) throw CustomException(ErrorCode.IMAGE_NOT_FOUND)
    }

    fun findUrlsByOwnerIdAndOwnerType(
        ownerType: ImageOwner,
        ownerId: Long,
    ): List<String> = imageRepository.findUrlsByOwnerTypeAndOwnerId(ownerType, ownerId)

    fun issue(
        type: ImageType,
        contentType: String,
    ): Pair<String, Long> {
        if (contentType !in ALLOWED_CONTENT_TYPES) {
            throw CustomException(ErrorCode.UNSUPPORTED_IMAGE_CONTENT_TYPE)
        }
        val (presignedUrl, publicUrl) = cloudStorageService.issuePresignedUrl(type, contentType)
        val imageId = imageRepository.save(publicUrl)
        return presignedUrl to imageId
    }

    fun saveImages(
        imageIds: List<Long>,
        ownerType: ImageOwner,
        ownerId: Long,
    ) {
        if (imageIds.isEmpty()) return
        imageRepository.saveOwners(ownerType, ownerId, imageIds)
    }

    fun generateQuoteShareImage(
        type: Int,
        createdAt: LocalDate,
        quote: String,
        title: String,
        author: String,
        coverImageUrl: String?,
    ): ByteArray =
        runCatching {
            SharedQuoteImageHelper.generate(
                type = type,
                createdAt = createdAt,
                quote = quote,
                title = title,
                author = author,
                coverImageUrl = coverImageUrl,
            )
        }.getOrElse { exception ->
            if (exception is IllegalArgumentException) {
                throw CustomException(ErrorCode.INVALID_INPUT)
            }
            throw exception
        }

    fun generateShareView2(
        createdAt: LocalDate,
        quote: String,
        title: String,
        author: String,
    ): ByteArray =
        SharedQuoteImageHelper.generateShareView2(
            createdAt = createdAt,
            quote = quote,
            title = title,
            author = author,
        )

    fun generateShareView1(
        createdAt: LocalDate,
        quote: String,
        title: String,
        author: String,
    ): ByteArray =
        SharedQuoteImageHelper.generateShareView1(
            createdAt = createdAt,
            quote = quote,
            title = title,
            author = author,
        )

    fun generateShareView3(
        createdAt: LocalDate,
        quote: String,
        title: String,
        author: String,
        coverImageUrl: String,
    ): ByteArray =
        SharedQuoteImageHelper.generateShareView3(
            createdAt = createdAt,
            quote = quote,
            title = title,
            author = author,
            coverImageUrl = coverImageUrl,
        )

    fun generateShareView4(
        month: LocalDate,
        books: Map<Int, List<String>>,
    ): ByteArray =
        SharedCalendarImageHelper.generateShareView4(
            month = month,
            books = books,
        )

    fun generateShareView5(
        month: LocalDate,
        books: Map<Int, List<String>>,
    ): ByteArray =
        SharedCalendarImageHelper.generateShareView5(
            month = month,
            books = books,
        )
}
