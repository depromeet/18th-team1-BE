package com.firstpenguin.app.domain.quotecreation.dto

import com.firstpenguin.app.domain.quotecreation.model.QuoteCreationBatchResultType

data class ParsedQuoteCreationBatchResult(
    val customId: String,
    val bookId: Long?,
    val resultType: QuoteCreationBatchResultType?,
    val quoteContents: List<String>,
    val acceptedCandidateIds: List<Long>,
    val errorMessage: String?,
)
