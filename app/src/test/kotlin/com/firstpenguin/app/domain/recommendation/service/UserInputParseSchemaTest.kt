package com.firstpenguin.app.domain.recommendation.service

import com.firstpenguin.app.domain.quotemetadata.dto.TagOption
import com.firstpenguin.app.global.enums.TagType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserInputParseSchemaTest {
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
    fun `schema는 canonicalIntent와 tag type별 후보 배열을 최상위 필수로 둔다`() {
        val schemaBody = userInputParseSchema(tagGroups)["schema"] as Map<*, *>
        val required = schemaBody["required"] as List<*>

        assertTrue(
            required.containsAll(
                listOf(
                    "canonicalIntent",
                    "needTagCandidates",
                    "situationTagCandidates",
                    "contextTagCandidates",
                    "roleTagCandidates",
                    "emotionTagCandidates",
                ),
            ),
        )
        assertTrue(required.indexOf("roleTagCandidates") < required.indexOf("emotionTagCandidates"))
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
    fun `emotion 후보 priority는 primary를 허용하지 않는다`() {
        val priorityEnum = priorityEnumOf("emotionTagCandidates")

        assertFalse(priorityEnum.contains("PRIMARY"))
        assertTrue(priorityEnum.contains("SECONDARY"))
        assertTrue(priorityEnum.contains("BACKGROUND"))
    }

    @Test
    fun `need 후보 priority는 primary를 허용한다`() {
        val priorityEnum = priorityEnumOf("needTagCandidates")

        assertTrue(priorityEnum.contains("PRIMARY"))
    }

    @Test
    fun `tag candidate schema는 confidence를 요구하지 않는다`() {
        val itemSchema = itemSchemaOf("needTagCandidates")
        val required = itemSchema["required"] as List<*>
        val properties = itemSchema["properties"] as Map<*, *>

        assertFalse(required.contains("confidence"))
        assertFalse(properties.containsKey("confidence"))
    }

    private fun priorityEnumOf(fieldName: String): List<*> {
        val itemSchema = itemSchemaOf(fieldName)
        val itemProperties = itemSchema["properties"] as Map<*, *>
        val prioritySchema = itemProperties["priority"] as Map<*, *>

        return prioritySchema["enum"] as List<*>
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
