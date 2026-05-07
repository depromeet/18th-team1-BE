package com.firstpenguin.app.global.config

import com.google.auth.ServiceAccountSigner
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["cloud.provider"], havingValue = "gcp")
class GcsConfig {
    @Bean
    fun gcsCredentials(): GoogleCredentials {
        val credentials = GoogleCredentials.getApplicationDefault()
        check(credentials is ServiceAccountSigner) {
            "GCS 자격증명이 서명을 지원하지 않습니다 (${credentials::class.simpleName}). 서비스 계정 자격증명을 사용하세요."
        }
        return credentials
    }

    @Bean
    fun gcsSigner(gcsCredentials: GoogleCredentials): ServiceAccountSigner = gcsCredentials as ServiceAccountSigner

    @Bean
    fun storage(gcsCredentials: GoogleCredentials): Storage =
        StorageOptions
            .newBuilder()
            .setCredentials(gcsCredentials)
            .build()
            .service
}
