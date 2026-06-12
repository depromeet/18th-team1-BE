package com.firstpenguin.app.domain.quotecreation.review.model

import com.firstpenguin.app.domain.book.model.Book

data class QuoteReviewBatchTarget(
    val book: Book,
    val candidates: List<QuoteCandidate>,
)
