package com.firstpenguin.app.domain.monthlysettlement.dto

import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementBook
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementEmotionTag
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementSelectedBook
import com.firstpenguin.app.domain.monthlysettlement.model.MonthlySettlementSnapshot

data class MonthlySettlementResponse(
    val year: Int,
    val month: Int,
    val sharedQuoteCount: Int,
    val mostFrequentGenre: String?,
    val monthlyBooks: List<MonthlySettlementBookResponse>,
    val emotionTags: List<MonthlySettlementEmotionTagResponse>,
    val recommendationMessage: String?,
    val monthlyBook: MonthlySettlementMonthlyBookResponse?,
) {
    companion object {
        fun empty(
            year: Int,
            month: Int,
        ): MonthlySettlementResponse =
            MonthlySettlementResponse(
                year = year,
                month = month,
                sharedQuoteCount = 0,
                mostFrequentGenre = null,
                monthlyBooks = emptyList(),
                emotionTags = emptyList(),
                recommendationMessage = null,
                monthlyBook = null,
            )

        fun from(snapshot: MonthlySettlementSnapshot): MonthlySettlementResponse =
            MonthlySettlementResponse(
                year = snapshot.settlement.year,
                month = snapshot.settlement.month,
                sharedQuoteCount = snapshot.settlement.sharedQuoteCount,
                mostFrequentGenre = snapshot.settlement.mostFrequentGenre,
                monthlyBooks = snapshot.monthlyBooks.map(MonthlySettlementBookResponse::from),
                emotionTags = snapshot.emotionTags.map(MonthlySettlementEmotionTagResponse::from),
                recommendationMessage = snapshot.settlement.recommendationMessage,
                monthlyBook = snapshot.settlement.monthlyBook?.let(MonthlySettlementMonthlyBookResponse::from),
            )
    }
}

data class MonthlySettlementBookResponse(
    val bookId: Long,
    val title: String,
    val author: String,
    val bookCoverImageUrl: String,
    val genre: String,
) {
    companion object {
        fun from(book: MonthlySettlementBook): MonthlySettlementBookResponse =
            MonthlySettlementBookResponse(
                bookId = book.bookId,
                title = book.title,
                author = book.author,
                bookCoverImageUrl = book.bookCoverImageUrl,
                genre = book.genre,
            )
    }
}

data class MonthlySettlementEmotionTagResponse(
    val tagId: Long,
    val label: String,
    val count: Int,
) {
    companion object {
        fun from(emotionTag: MonthlySettlementEmotionTag): MonthlySettlementEmotionTagResponse =
            MonthlySettlementEmotionTagResponse(
                tagId = emotionTag.tagId,
                label = emotionTag.label,
                count = emotionTag.count,
            )
    }
}

data class MonthlySettlementMonthlyBookResponse(
    val quoteId: Long,
    val bookId: Long,
    val quoteContent: String,
    val title: String,
    val author: String,
    val bookCoverImageUrl: String,
    val genre: String,
) {
    companion object {
        fun from(monthlyBook: MonthlySettlementSelectedBook): MonthlySettlementMonthlyBookResponse =
            MonthlySettlementMonthlyBookResponse(
                quoteId = monthlyBook.quoteId,
                bookId = monthlyBook.bookId,
                quoteContent = monthlyBook.quoteContent,
                title = monthlyBook.title,
                author = monthlyBook.author,
                bookCoverImageUrl = monthlyBook.bookCoverImageUrl,
                genre = monthlyBook.genre,
            )
    }
}
