package com.firstpenguin.app.domain.batch.dto.ai

import com.firstpenguin.app.global.enums.BatchJobStatus

data class OpenAiBatchResponse(
    val id: String,
    val status: BatchJobStatus,
)
