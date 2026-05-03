package com.firstpenguin.app.domain.book.service

import com.firstpenguin.app.domain.book.model.Book
import com.firstpenguin.app.domain.book.repository.BookRepository
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service

@Service
class BookService(
    private val bookRepository: BookRepository
) {
    fun findBookById(id: Long): Book =
        bookRepository.findBookById(id)
            ?: throw CustomException(ErrorCode.BOOK_NOT_FOUND)
}
