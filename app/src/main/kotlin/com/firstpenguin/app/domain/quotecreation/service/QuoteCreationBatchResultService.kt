package com.firstpenguin.app.domain.quotecreation.service

import com.firstpenguin.app.domain.quote.model.QuoteSourceType
import com.firstpenguin.app.domain.quote.repository.QuoteRepository
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchItem
import com.firstpenguin.app.domain.quotebatch.repository.QuoteBatchItemRepository
import com.firstpenguin.app.domain.quotebatch.repository.QuoteBatchJobRepository
import com.firstpenguin.app.domain.quotecreation.dto.ParsedQuoteCreationBatchResult
import com.firstpenguin.app.domain.quotecreation.model.QuoteCreationBatchResultType
import com.firstpenguin.app.domain.quotecreation.review.model.QuoteCandidate
import com.firstpenguin.app.domain.quotecreation.review.repository.QuoteCandidateRepository
import com.firstpenguin.app.global.enums.BatchItemStatus
import org.springframework.stereotype.Service

private const val MAX_EXTRACTED_QUOTE_COUNT = 3
private const val MIN_REVIEWED_QUOTE_LENGTH = 10
private const val MAX_REVIEWED_QUOTE_LENGTH = 90
private val HAN_CHARACTER_REGEX = Regex("[\\u4E00-\\u9FFF]")

@Service
class QuoteCreationBatchResultService(
    private val quoteRepository: QuoteRepository,
    private val jobRepository: QuoteBatchJobRepository,
    private val itemRepository: QuoteBatchItemRepository,
    private val candidateRepository: QuoteCandidateRepository,
) {
    fun saveBatchResults(
        jobId: Long,
        results: List<ParsedQuoteCreationBatchResult>,
    ) {
        results.forEach { result -> saveBatchResult(jobId, result) }
        updateJobCounts(jobId)
    }

    private fun saveBatchResult(
        jobId: Long,
        result: ParsedQuoteCreationBatchResult,
    ) {
        val item = result.findBatchItem(jobId)
        if (item == null || item.status !in BatchItemStatus.activeStatuses()) return
        if (result.errorMessage != null) {
            markItemFailed(item, result.errorMessage)
        }
        if (result.errorMessage == null) {
            saveSuccessfulResultByType(item, result)
        }
    }

    private fun saveSuccessfulResultByType(
        item: QuoteBatchItem,
        result: ParsedQuoteCreationBatchResult,
    ) {
        if (result.resultType == QuoteCreationBatchResultType.QUOTE_REVIEW) {
            saveCandidateReviewResult(item, result)
        }
        if (result.resultType == QuoteCreationBatchResultType.QUOTE_EXTRACTION) {
            saveQuoteContents(item, result.quoteContents, QuoteSourceType.BOOK_INSPIRED)
        }
    }

    private fun saveCandidateReviewResult(
        item: QuoteBatchItem,
        result: ParsedQuoteCreationBatchResult,
    ) {
        val pendingCandidates = candidateRepository.findPendingCandidatesByBookId(item.targetId)
        val acceptedCandidates = result.acceptedCandidates(pendingCandidates)
        saveQuoteContents(
            item = item,
            quoteContents = acceptedCandidates.map { candidate -> candidate.content },
            sourceType = QuoteSourceType.CANDIDATE_REVIEWED,
        )
        markReviewedCandidates(pendingCandidates, acceptedCandidates)
    }

    private fun saveQuoteContents(
        item: QuoteBatchItem,
        quoteContents: List<String>,
        sourceType: QuoteSourceType,
    ) {
        quoteContents.forEach { content -> quoteRepository.insertQuote(item.targetId, content, sourceType) }
        markItemSucceeded(item)
    }

    private fun markReviewedCandidates(
        pendingCandidates: List<QuoteCandidate>,
        acceptedCandidates: List<QuoteCandidate>,
    ) {
        val acceptedIds = acceptedCandidates.map { candidate -> candidate.id }
        val rejectedIds = rejectedCandidateIds(pendingCandidates, acceptedCandidates)
        candidateRepository.markCandidatesAccepted(acceptedIds)
        candidateRepository.markCandidatesRejected(rejectedIds)
    }

    private fun ParsedQuoteCreationBatchResult.findBatchItem(jobId: Long): QuoteBatchItem? =
        bookId?.let { resultBookId ->
            itemRepository.findByJobIdAndTargetId(jobId, resultBookId)
        }

    private fun markItemSucceeded(item: QuoteBatchItem) =
        itemRepository.updateQuoteBatchItemStatusByTargetId(
            jobId = item.jobId,
            targetId = item.targetId,
            status = BatchItemStatus.SUCCEEDED,
        )

    private fun markItemFailed(
        item: QuoteBatchItem,
        errorMessage: String?,
    ) = itemRepository.updateQuoteBatchItemStatusByTargetId(
        jobId = item.jobId,
        targetId = item.targetId,
        status = BatchItemStatus.FAILED,
        errorMessage = errorMessage,
    )

    private fun updateJobCounts(jobId: Long) {
        jobRepository.updateQuoteBatchJobCounts(
            jobId = jobId,
            succeededCount = itemRepository.countItemsByStatus(jobId, BatchItemStatus.SUCCEEDED),
            failedCount = itemRepository.countItemsByStatus(jobId, BatchItemStatus.FAILED),
        )
    }
}

private fun rejectedCandidateIds(
    pendingCandidates: List<QuoteCandidate>,
    acceptedCandidates: List<QuoteCandidate>,
): List<Long> {
    val acceptedIds = acceptedCandidates.map { candidate -> candidate.id }.toSet()
    return pendingCandidates
        .map { candidate -> candidate.id }
        .filterNot { candidateId -> candidateId in acceptedIds }
}

private fun ParsedQuoteCreationBatchResult.acceptedCandidates(pending: List<QuoteCandidate>): List<QuoteCandidate> =
    acceptedCandidateIds
        .distinct()
        .take(MAX_EXTRACTED_QUOTE_COUNT)
        .mapNotNull { candidateId ->
            pending.firstOrNull { candidate -> candidate.id == candidateId }
        }.filter { candidate -> candidate.isAcceptableReviewedQuote() }

private fun QuoteCandidate.isAcceptableReviewedQuote(): Boolean {
    val trimmedContent = content.trimQuoteMarks()
    val validLengthRange = MIN_REVIEWED_QUOTE_LENGTH..MAX_REVIEWED_QUOTE_LENGTH

    val isValidLength = trimmedContent.length in validLengthRange
    val hasNoHanCharacter = !HAN_CHARACTER_REGEX.containsMatchIn(trimmedContent)
    val hasSingleTerminalPunctuation = trimmedContent.terminalPunctuationCount() <= 1

    return isValidLength &&
        hasNoHanCharacter &&
        hasSingleTerminalPunctuation
}

private fun String.trimQuoteMarks(): String =
    trim()
        .trim('"', '\'', '“', '”', '‘', '’')
        .trim()

private fun String.terminalPunctuationCount(): Int =
    count { character ->
        character == '.' || character == '?' || character == '!'
    }
