package com.firstpenguin.app.domain.recommendation.service

private const val CANONICAL_INTENT_MIN_LENGTH = 10
private const val CANONICAL_INTENT_MAX_LENGTH = 120

internal fun userCanonicalIntentSchema(): Map<String, Any> =
    mapOf(
        "type" to "json_schema",
        "name" to "user_canonical_intent",
        "strict" to true,
        "schema" to
            mapOf(
                "type" to "object",
                "additionalProperties" to false,
                "required" to listOf("canonicalIntent"),
                "properties" to
                    mapOf(
                        "canonicalIntent" to
                            mapOf(
                                "type" to "string",
                                "description" to "사용자의 현재 마음과 원하는 도움을 요약한 한국어 한 문장",
                                "minLength" to CANONICAL_INTENT_MIN_LENGTH,
                                "maxLength" to CANONICAL_INTENT_MAX_LENGTH,
                            ),
                    ),
            ),
    )
