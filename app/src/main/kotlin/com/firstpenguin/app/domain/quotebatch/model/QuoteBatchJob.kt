<<<<<<<< HEAD:app/src/main/kotlin/com/firstpenguin/app/domain/quotebatch/model/QuoteBatchJob.kt
package com.firstpenguin.app.domain.quotebatch.model
========
package com.firstpenguin.app.domain.quotemetadata.model
>>>>>>>> origin/dev:app/src/main/kotlin/com/firstpenguin/app/domain/quotemetadata/model/QuoteMetadataBatchJob.kt

import com.firstpenguin.app.global.enums.BatchJobStatus
import java.time.LocalDateTime

data class QuoteBatchJob(
    val id: Long,
    val openAiBatchId: String?,
    val inputFileId: String?,
    val outputFileId: String?,
    val errorFileId: String?,
    val status: BatchJobStatus,
    val jobType: QuoteBatchType,
    val model: String,
    val version: Int,
    val submittedCount: Int,
    val succeededCount: Int,
    val failedCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val completedAt: LocalDateTime?,
)
