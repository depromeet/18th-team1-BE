package com.firstpenguin.app.domain.quotecreation.review.service

import com.firstpenguin.app.domain.book.model.Book
import com.firstpenguin.app.domain.quotecreation.review.model.QuoteCandidate

private const val RECOMMENDED_CANDIDATE_COUNT = 3

internal fun quoteReviewSchema(
    book: Book,
    candidates: List<QuoteCandidate>,
): Map<String, Any> =
    mapOf(
        "type" to "json_schema",
        "name" to "quote_review",
        "strict" to true,
        "schema" to quoteReviewSchemaBody(book, candidates),
    )

private fun quoteReviewSchemaBody(
    book: Book,
    candidates: List<QuoteCandidate>,
): Map<String, Any> =
    mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "required" to listOf("bookId", "acceptedCandidateIds"),
        "properties" to
            mapOf(
                "bookId" to mapOf("type" to "integer", "enum" to listOf(book.id)),
                "acceptedCandidateIds" to candidateIdArraySchema(candidates),
            ),
    )

private fun candidateIdArraySchema(candidates: List<QuoteCandidate>): Map<String, Any> =
    mapOf(
        "type" to "array",
        "maxItems" to RECOMMENDED_CANDIDATE_COUNT,
        "items" to mapOf("type" to "integer", "enum" to candidates.map { candidate -> candidate.id }),
    )
