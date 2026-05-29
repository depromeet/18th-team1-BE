package com.firstpenguin.app.domain.batch.service

import com.firstpenguin.app.domain.batch.dto.TagOption
import com.firstpenguin.app.domain.quote.model.Quote
import com.firstpenguin.app.global.enums.QuoteMetadataBatchModelVersion
import com.firstpenguin.app.global.enums.TagType
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class QuoteMetadataBatchJsonlBuilder(
    private val jsonMapper: JsonMapper,
) {
    fun build(
        quotes: List<Quote>,
        tagGroups: Map<TagType, List<TagOption>>,
    ): String =
        quotes.joinToString("\n") { quote ->
            jsonMapper.writeValueAsString(
                mapOf(
                    "custom_id" to "quote-${quote.id}",
                    "method" to "POST",
                    "url" to "/v1/responses",
                    "body" to
                        mapOf(
                            "model" to QuoteMetadataBatchModelVersion.V1.model,
                            "input" to buildPrompt(quote, tagGroups),
                            "text" to
                                mapOf(
                                    "format" to quoteMetadataSchema(tagGroups),
                                ),
                        ),
                ),
            )
        }

    private fun quoteMetadataSchema(tagGroups: Map<TagType, List<TagOption>>): Map<String, Any> =
        mapOf(
            "type" to "json_schema",
            "name" to "quote_metadata",
            "strict" to true,
            "schema" to
                mapOf(
                    "type" to "object",
                    "additionalProperties" to false,
                    "required" to
                        listOf(
                            "quoteId",
                            "emotionTagCodes",
                            "needTagCodes",
                            "situationTagCodes",
                            "contextTagCodes",
                            "moodTagCodes",
                            "roleTagCodes",
                            "avoidTagCodes",
                            "embeddingText",
                        ),
                    "properties" to
                        mapOf(
                            "quoteId" to mapOf("type" to "integer"),
                            "emotionTagCodes" to codeArraySchema(tagGroups.getValue(TagType.EMOTION)),
                            "needTagCodes" to codeArraySchema(tagGroups.getValue(TagType.NEED)),
                            "situationTagCodes" to codeArraySchema(tagGroups.getValue(TagType.SITUATION)),
                            "contextTagCodes" to codeArraySchema(tagGroups.getValue(TagType.CONTEXT)),
                            "moodTagCodes" to codeArraySchema(tagGroups.getValue(TagType.MOOD)),
                            "roleTagCodes" to codeArraySchema(tagGroups.getValue(TagType.ROLE)),
                            "avoidTagCodes" to codeArraySchema(tagGroups.getValue(TagType.AVOID)),
                            "embeddingText" to mapOf("type" to "string"),
                        ),
                ),
        )

    private fun codeArraySchema(options: List<TagOption>): Map<String, Any> =
        mapOf(
            "type" to "array",
            "items" to
                mapOf(
                    "type" to "string",
                    "enum" to options.map { it.code },
                ),
        )

    private fun buildPrompt(
        quote: Quote,
        tagGroups: Map<TagType, List<TagOption>>,
    ): String =
        """
아래 인용구를 추천 메타데이터로 분석해 JSON으로만 응답해줘.
반드시 제공된 tag code 중에서만 선택해.

[EMOTION]
${tagOptionsText(tagGroups.getValue(TagType.EMOTION))}

[NEED]
${tagOptionsText(tagGroups.getValue(TagType.NEED))}

[SITUATION]
${tagOptionsText(tagGroups.getValue(TagType.SITUATION))}

[CONTEXT]
${tagOptionsText(tagGroups.getValue(TagType.CONTEXT))}

[MOOD]
${tagOptionsText(tagGroups.getValue(TagType.MOOD))}

[ROLE]
${tagOptionsText(tagGroups.getValue(TagType.ROLE))}

[AVOID]
${tagOptionsText(tagGroups.getValue(TagType.AVOID))}

quoteId: ${quote.id}
content: ${quote.content}
        """.trimIndent()

    private fun tagOptionsText(options: List<TagOption>): String = options.joinTags()

    private fun List<TagOption>.joinTags(): String = joinToString(separator = "\n", transform = ::tagOptionText)

    private fun tagOptionText(option: TagOption): String = "- ${option.code}: ${option.label}"
}
