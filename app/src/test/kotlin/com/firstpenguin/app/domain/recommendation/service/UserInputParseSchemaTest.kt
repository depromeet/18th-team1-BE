package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.quotemetadata.dto.TagOption
import com.firstpenguin.app.global.enums.TagType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserInputParseSchemaTest {
    @Test
    fun `canonical schema는 canonicalIntent만 필수로 둔다`() {
        val schemaBody = userCanonicalIntentSchema()["schema"] as Map<*, *>
        val required = schemaBody["required"] as List<*>
        val properties = schemaBody["properties"] as Map<*, *>

        assertTrue(required.contains("canonicalIntent"))
        assertTrue(properties.containsKey("canonicalIntent"))
        assertFalse(properties.containsKey("needTagCandidates"))
    }

    @Test
    fun `schema는 mood와 avoid를 반환 대상에 포함하지 않는다`() {
        val schema = userInputParseSchema(tagGroups).toString()

        assertFalse(schema.contains("MOOD"))
        assertFalse(schema.contains("AVOID"))
        assertTrue(schema.contains("EMOTION"))
        assertTrue(schema.contains("NEED"))
        assertTrue(schema.contains("SITUATION"))
        assertTrue(schema.contains("CONTEXT"))
        assertTrue(schema.contains("ROLE"))
        assertFalse(schema.contains("evidence"))
    }

    @Test
    fun `schema는 tag type별 후보 배열을 최상위 필수로 둔다`() {
        val schemaBody = userInputParseSchema(tagGroups)["schema"] as Map<*, *>
        val required = schemaBody["required"] as List<*>

        assertTrue(
            required.containsAll(
                listOf(
                    "needTagCandidates",
                    "situationTagCandidates",
                    "contextTagCandidates",
                    "roleTagCandidates",
                    "emotionTagCandidates",
                ),
            ),
        )
        assertTrue(required.indexOf("roleTagCandidates") < required.indexOf("emotionTagCandidates"))
        assertFalse(required.contains("canonicalIntent"))
        assertFalse(required.contains("intentType"))
        assertFalse(required.contains("tagCandidates"))
        assertFalse(required.contains("quoteId"))
    }

    @Test
    fun `schema는 허용 tagCode를 tag type별 후보 배열 안에 나눠서 둔다`() {
        val schema = userInputParseSchema(tagGroups).toString()

        assertTrue(schema.contains("emotionTagCandidates"))
        assertTrue(schema.contains("EMOTION_CODE"))
        assertTrue(schema.contains("needTagCandidates"))
        assertTrue(schema.contains("NEED_CODE"))
        assertTrue(schema.indexOf("NEED_CODE") < schema.indexOf("EMOTION_CODE"))
    }

    @Test
    fun `tag candidate schema는 priority를 요구하지 않는다`() {
        val itemSchema = itemSchemaOf("emotionTagCandidates")
        val required = itemSchema["required"] as List<*>
        val properties = itemSchema["properties"] as Map<*, *>

        assertFalse(required.contains("priority"))
        assertFalse(properties.containsKey("priority"))
    }

    @Test
    fun `need tag group이 없으면 need 후보 필드를 요구하지 않는다`() {
        val schemaBody = userInputParseSchema(tagGroups - TagType.NEED)["schema"] as Map<*, *>
        val required = schemaBody["required"] as List<*>
        val properties = schemaBody["properties"] as Map<*, *>

        assertFalse(required.contains("needTagCandidates"))
        assertFalse(properties.containsKey("needTagCandidates"))
    }

    @Test
    fun `tag candidate schema는 confidence와 priority를 요구하지 않는다`() {
        val itemSchema = itemSchemaOf("needTagCandidates")
        val required = itemSchema["required"] as List<*>
        val properties = itemSchema["properties"] as Map<*, *>

        assertFalse(required.contains("confidence"))
        assertFalse(required.contains("priority"))
        assertFalse(properties.containsKey("confidence"))
        assertFalse(properties.containsKey("priority"))
    }

    private fun itemSchemaOf(fieldName: String): Map<*, *> {
        val schemaBody = userInputParseSchema(tagGroups)["schema"] as Map<*, *>
        val properties = schemaBody["properties"] as Map<*, *>
        val candidateSchema = properties[fieldName] as Map<*, *>

        return candidateSchema["items"] as Map<*, *>
    }

    private companion object {
        val tagGroups: Map<TagType, List<TagOption>> =
            TagType.entries.associateWith { type ->
                listOf(
                    TagOption(
                        id = type.ordinal.toLong() + 1,
                        type = type,
                        code = "${type.name}_CODE",
                        label = type.name,
                        description = null,
                    ),
                )
            }
    }
}
