package com.firstpenguin.app.domain.batch.dto

import com.firstpenguin.app.domain.batch.model.QuoteMetadata
import com.firstpenguin.app.domain.batch.model.QuoteMetadataTag

data class ParsedBatchQuoteResult(
    val customId: String,
    val quoteId: Long?,
    val roleTagCode: String?,
    val emotionTagCodes: List<String>?,
    val needTagCodes: List<String>?,
    val situationTagCodes: List<String>?,
    val contextTagCodes: List<String>?,
    val moodTagCodes: List<String>?,
    val embeddingText: String?,
    val errorMessage: String?,
) {
    fun toQuoteMetadata(
        metadataModel: String,
        metadataVersion: Int,
    ): QuoteMetadata =
        QuoteMetadata(
            quoteId = requireValue(quoteId, "quoteId"),
            embeddingText = requireValue(embeddingText, "embeddingText"),
            metadataModel = metadataModel,
            metadataVersion = metadataVersion,
        )

    fun toQuoteMetadataTags(
        quoteMetadataId: Long,
        tagIdByCode: Map<String, Long>,
    ): List<QuoteMetadataTag> =
        tagCodes()
            .map { tagCode -> tagCode.toQuoteMetadataTag(quoteMetadataId, tagIdByCode) }

    private fun tagCodes(): List<String> =
        listOfNotNull(roleTagCode)
            .asSequence()
            .plus(emotionTagCodes.orEmpty())
            .plus(needTagCodes.orEmpty())
            .plus(situationTagCodes.orEmpty())
            .plus(contextTagCodes.orEmpty())
            .plus(moodTagCodes.orEmpty())
            .distinct()
            .toList()

    private fun String.toQuoteMetadataTag(
        quoteMetadataId: Long,
        tagIdByCode: Map<String, Long>,
    ): QuoteMetadataTag =
        QuoteMetadataTag(
            quoteMetadataId = quoteMetadataId,
            tagId = requireValue(tagIdByCode[this], this),
        )

    private fun <T> requireValue(
        value: T?,
        fieldName: String,
    ): T = requireNotNull(value) { "$customId has no $fieldName" }
}
