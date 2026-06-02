package com.firstpenguin.app.domain.batch.dto.ai

import com.firstpenguin.app.global.enums.BatchJobStatus

data class OpenAiBatchStatusResponse(
    val id: String,
    val status: BatchJobStatus,
    val outputFileId: String?,
    val errorFileId: String?,
)
