package com.firstpenguin.app.domain.home.userCase

import com.firstpenguin.app.domain.book.service.BookService
import com.firstpenguin.app.domain.image.service.ImageService
import com.firstpenguin.app.domain.quote.dto.QuoteResponse
import com.firstpenguin.app.domain.quote.service.QuoteService
import com.firstpenguin.app.global.enums.ImageOwner
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class HomeUserCase(
    private val quoteService: QuoteService,
    private val bookService: BookService,
    private val imageService: ImageService,
) {
    @Transactional(readOnly = true)
    fun getRandomQuote(): QuoteResponse {
        val randomQuote = quoteService.getRandomQuote()

        val book = bookService.findBookById(randomQuote.bookId)
        val bookCoverImage = imageService.findUrlsByOwnerIdAndOwnerType(ImageOwner.BOOK, randomQuote.bookId).first()

        return QuoteResponse(
            quoteId = randomQuote.id,
            bookId = book.id,
            content = randomQuote.content,
            title = book.title,
            author = book.author,
            publisher = book.publisher,
            image = bookCoverImage,
        )
    }
}
