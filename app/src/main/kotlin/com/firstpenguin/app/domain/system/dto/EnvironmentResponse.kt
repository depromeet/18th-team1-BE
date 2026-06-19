package com.firstpenguin.app.domain.system.dto

data class EnvironmentResponse(
    val activeProfiles: List<String>,
    val prod: Boolean,
    val devTokenEnabled: Boolean,
)
