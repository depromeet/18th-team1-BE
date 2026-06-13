package com.firstpenguin.app.domain.quote.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

private const val MAX_BULK_DELETE_QUOTE_COUNT = 50

@Schema(description = "문장 스크랩 다중 취소 요청")
data class BulkDeleteQuoteScrapsRequest(
    @field:Schema(
        description = "스크랩 취소할 문장 ID 목록. 1 이상 정수만 허용하며 최대 50개까지 요청할 수 있습니다.",
        example = "[1, 2, 3]",
    )
    @field:NotEmpty(message = "스크랩 취소할 문장 ID를 1개 이상 입력해주세요.")
    @field:Size(max = MAX_BULK_DELETE_QUOTE_COUNT, message = "한 번에 최대 50개까지 취소할 수 있습니다.")
    val quoteIds: List<Long>,
)
