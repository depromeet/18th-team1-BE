package com.firstpenguin.app.domain.recommendation.service

import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

@Component
class UserCanonicalIntentOutputParser(
    private val objectMapper: ObjectMapper,
) {
    fun parse(outputText: String): String =
        objectMapper
            .readTree(outputText)
            .requiredText("canonicalIntent")
}

private fun JsonNode.requiredText(fieldName: String): String =
    path(fieldName)
        .takeUnless { node -> node.isMissingNode || node.isNull }
        ?.asString()
        ?.takeIf { value -> value.isNotBlank() }
        ?: error("Missing required text field: $fieldName")
