package com.firstpenguin.app.domain.quote.dto

import com.firstpenguin.app.domain.book.model.Book
import com.firstpenguin.app.domain.quote.model.Quote

data class QuoteResponse(
    val quoteId: Long,
    val bookId: Long,
    val content: String,
    val title: String,
    val author: String,
    val image: String,
) {
    companion object {
        fun from(
            quote: Quote,
            book: Book,
        ): QuoteResponse =
            QuoteResponse(
                quoteId = quote.id,
                bookId = book.id,
                content = quote.content,
                title = book.title,
                author = book.author,
                image = book.coverImageUrl,
            )
    }
}
