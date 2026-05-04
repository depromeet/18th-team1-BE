package com.firstpenguin.app

import com.firstpenguin.app.domain.image.service.CloudStorageService
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
class AppApplicationTests {
    @MockitoBean
    lateinit var cloudStorageService: CloudStorageService

    @Test
    fun contextLoads() {
        // Spring application context loads successfully
    }
}
