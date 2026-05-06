package com.firstpenguin.app.global.config

import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["cloud.provider"], havingValue = "gcp")
class GcsConfig {
    @Bean
    fun storage(): Storage = StorageOptions.getDefaultInstance().service
}
