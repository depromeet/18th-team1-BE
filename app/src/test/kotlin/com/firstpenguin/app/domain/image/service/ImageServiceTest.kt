package com.firstpenguin.app.domain.image.service

import com.firstpenguin.app.domain.image.model.ImageType
import com.firstpenguin.app.domain.image.repository.ImageRepository
import com.firstpenguin.app.global.enums.ImageOwner
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class ImageServiceTest {
    @Mock
    lateinit var imageRepository: ImageRepository

    @Mock
    lateinit var cloudStorageService: CloudStorageService

    @InjectMocks
    lateinit var imageService: ImageService

    @Test
    fun `issue - 허용된 contentType이면 presignedUrl과 imageId를 반환한다`() {
        val presignedUrl = "https://storage.googleapis.com/presigned"
        val publicUrl = "https://storage.googleapis.com/bucket/diary/uuid.jpg"
        val imageId = 1L

        Mockito
            .`when`(cloudStorageService.issuePresignedUrl(ImageType.DIARY, "image/jpeg"))
            .thenReturn(presignedUrl to publicUrl)
        Mockito.`when`(imageRepository.save(publicUrl)).thenReturn(imageId)

        val (resultPresignedUrl, resultImageId) = imageService.issue(ImageType.DIARY, "image/jpeg")

        assertEquals(presignedUrl, resultPresignedUrl)
        assertEquals(imageId, resultImageId)
    }

    @Test
    fun `issue - 허용되지 않은 contentType이면 예외를 던진다`() {
        val exception =
            assertThrows(CustomException::class.java) {
                imageService.issue(ImageType.DIARY, "image/gif")
            }

        assertEquals(ErrorCode.UNSUPPORTED_IMAGE_CONTENT_TYPE, exception.errorCode)
        Mockito.verifyNoInteractions(cloudStorageService, imageRepository)
    }

    @Test
    fun `saveImages - imageIds가 있으면 saveOwners를 호출한다`() {
        val imageIds = listOf(1L, 2L, 3L)

        imageService.saveImages(imageIds, ImageOwner.DIARY, 10L)

        Mockito.verify(imageRepository).saveOwners(ImageOwner.DIARY, 10L, imageIds)
    }

    @Test
    fun `saveImages - imageIds가 비어있으면 saveOwners를 호출하지 않는다`() {
        imageService.saveImages(emptyList(), ImageOwner.DIARY, 10L)

        Mockito.verifyNoInteractions(imageRepository)
    }
}
