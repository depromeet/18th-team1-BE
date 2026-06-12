package com.firstpenguin.app.domain.discovery.model

import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

data class DiscoveryCursor(
    val recommendedAt: LocalDateTime,
    val quoteId: Long,
) {
    fun encode(): String {
        val rawCursor =
            listOf(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(recommendedAt),
                quoteId,
            ).joinToString(DELIMITER)

        return encoder.encodeToString(rawCursor.toByteArray(Charsets.UTF_8))
    }

    companion object {
        fun from(quote: DiscoveryQuote): DiscoveryCursor =
            DiscoveryCursor(
                recommendedAt = quote.recommendedAt,
                quoteId = quote.quoteId,
            )

        fun parse(cursor: String?): DiscoveryCursor? {
            if (cursor.isNullOrBlank()) return null

            return runCatching { decode(cursor) }
                .getOrElse { throw CustomException(ErrorCode.INVALID_INPUT) }
        }

        private fun decode(cursor: String): DiscoveryCursor {
            val parts = String(decoder.decode(cursor), Charsets.UTF_8).split(DELIMITER)
            if (parts.size != CURSOR_PART_COUNT) {
                throw IllegalArgumentException("Invalid discovery cursor format")
            }

            return DiscoveryCursor(
                recommendedAt = LocalDateTime.parse(parts[RECOMMENDED_AT_INDEX], DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                quoteId = parts[QUOTE_ID_INDEX].toLong(),
            )
        }

        private const val DELIMITER = "|"
        private const val CURSOR_PART_COUNT = 2
        private const val RECOMMENDED_AT_INDEX = 0
        private const val QUOTE_ID_INDEX = 1

        private val encoder = Base64.getUrlEncoder().withoutPadding()
        private val decoder = Base64.getUrlDecoder()
    }
}
