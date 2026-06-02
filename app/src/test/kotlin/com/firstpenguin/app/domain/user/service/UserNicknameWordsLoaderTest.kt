package com.firstpenguin.app.domain.user.service

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserNicknameWordsLoaderTest {
    @Test
    fun `닉네임 단어 목록을 리소스에서 불러온다`() {
        val words = UserNicknameWordsLoader().load()

        assertEquals(71, words.modifiers.size)
        assertEquals(75, words.animals.size)
        assertTrue(words.modifiers.contains("책읽는"))
        assertTrue(words.animals.contains("펭귄"))
    }
}
