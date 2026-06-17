package com.firstpenguin.app.domain.discovery.model

import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

data class DiscoveryQuoteSearchCursor(
    val recommendedAt: LocalDateTime,
    val quoteId: Long,
    val scrapCount: Int?,
) {
    fun encode(sort: DiscoveryQuoteSearchSort): String {
        val rawCursor = rawCursor(sort)

        return encoder.encodeToString(rawCursor.toByteArray(Charsets.UTF_8))
    }

    private fun rawCursor(sort: DiscoveryQuoteSearchSort): String =
        when (sort) {
            DiscoveryQuoteSearchSort.LATEST -> latestCursorParts().joinToString(DELIMITER)
            DiscoveryQuoteSearchSort.SCRAP_COUNT -> scrapCountCursorParts().joinToString(DELIMITER)
        }

    private fun latestCursorParts(): List<Any> =
        listOf(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(recommendedAt),
            quoteId,
        )

    private fun scrapCountCursorParts(): List<Any> =
        listOf(
            requireNotNull(scrapCount),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(recommendedAt),
            quoteId,
        )

    companion object {
        fun from(
            quote: DiscoveryQuote,
            sort: DiscoveryQuoteSearchSort,
        ): DiscoveryQuoteSearchCursor =
            DiscoveryQuoteSearchCursor(
                recommendedAt = quote.recommendedAt,
                quoteId = quote.quoteId,
                scrapCount = quote.scrapCount.takeIf { sort == DiscoveryQuoteSearchSort.SCRAP_COUNT },
            )

        fun parse(
            cursor: String?,
            sort: DiscoveryQuoteSearchSort,
        ): DiscoveryQuoteSearchCursor? {
            if (cursor.isNullOrBlank()) return null

            return runCatching { decode(cursor, sort) }
                .getOrElse { throw CustomException(ErrorCode.INVALID_INPUT) }
        }

        private fun decode(
            cursor: String,
            sort: DiscoveryQuoteSearchSort,
        ): DiscoveryQuoteSearchCursor {
            val parts = String(decoder.decode(cursor), Charsets.UTF_8).split(DELIMITER)
            return when (sort) {
                DiscoveryQuoteSearchSort.LATEST -> decodeLatest(parts)
                DiscoveryQuoteSearchSort.SCRAP_COUNT -> decodeScrapCount(parts)
            }
        }

        private fun decodeLatest(parts: List<String>): DiscoveryQuoteSearchCursor {
            if (parts.size != LATEST_CURSOR_PART_COUNT) {
                throw IllegalArgumentException("Invalid latest search cursor format")
            }

            return DiscoveryQuoteSearchCursor(
                recommendedAt = parseAt(parts[LATEST_RECOMMENDED_AT_INDEX]),
                quoteId = parts[LATEST_QUOTE_ID_INDEX].toLong(),
                scrapCount = null,
            )
        }

        private fun decodeScrapCount(parts: List<String>): DiscoveryQuoteSearchCursor {
            if (parts.size != SCRAP_COUNT_CURSOR_PART_COUNT) {
                throw IllegalArgumentException("Invalid scrap count search cursor format")
            }

            return DiscoveryQuoteSearchCursor(
                scrapCount = parts[SCRAP_COUNT_INDEX].toInt(),
                recommendedAt = parseAt(parts[SCRAP_COUNT_RECOMMENDED_AT_INDEX]),
                quoteId = parts[SCRAP_COUNT_QUOTE_ID_INDEX].toLong(),
            )
        }

        private fun parseAt(value: String): LocalDateTime {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            return LocalDateTime.parse(value, formatter)
        }

        private const val DELIMITER = "|"
        private const val LATEST_CURSOR_PART_COUNT = 2
        private const val SCRAP_COUNT_CURSOR_PART_COUNT = 3
        private const val LATEST_RECOMMENDED_AT_INDEX = 0
        private const val LATEST_QUOTE_ID_INDEX = 1
        private const val SCRAP_COUNT_INDEX = 0
        private const val SCRAP_COUNT_RECOMMENDED_AT_INDEX = 1
        private const val SCRAP_COUNT_QUOTE_ID_INDEX = 2

        private val encoder = Base64.getUrlEncoder().withoutPadding()
        private val decoder = Base64.getUrlDecoder()
    }
}
