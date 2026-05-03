package com.firstpenguin.app.global.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "gcs")
data class GcsProperties (
    @field:NotBlank
    var bucketName: String = "temp",
    @field:NotBlank
    var baseUrl: String = "",
)