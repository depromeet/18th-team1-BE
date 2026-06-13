package com.firstpenguin.app.domain.quote.dto

import com.firstpenguin.app.domain.quote.model.ScrappedQuote
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "마이페이지 스크랩 문장 응답")
data class ScrappedQuoteResponse(
    @field:Schema(description = "문장 ID", example = "1")
    val quoteId: Long,
    @field:Schema(description = "책 ID", example = "10")
    val bookId: Long,
    @field:Schema(description = "책 표지 이미지 URL", example = "https://cdn.example.com/book-cover.png")
    val bookCoverImageUrl: String,
    @field:Schema(description = "문장 내용", example = "새는 알에서 나오려고 투쟁한다.")
    val content: String,
    @field:Schema(description = "책 제목", example = "데미안")
    val title: String,
    @field:Schema(description = "작가", example = "헤르만 헤세")
    val author: String,
    @field:Schema(description = "스크랩한 시각", example = "2026-06-13T14:30:00")
    val scrappedAt: LocalDateTime,
) {
    companion object {
        fun from(quote: ScrappedQuote): ScrappedQuoteResponse =
            ScrappedQuoteResponse(
                quoteId = quote.quoteId,
                bookId = quote.bookId,
                bookCoverImageUrl = quote.bookCoverImageUrl,
                content = quote.content,
                title = quote.title,
                author = quote.author,
                scrappedAt = quote.scrappedAt,
            )
    }
}
