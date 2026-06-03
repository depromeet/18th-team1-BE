package com.firstpenguin.app.domain.user.service

import org.springframework.stereotype.Component

@Component
class UserNicknameGenerator(
    userNicknameWordsLoader: UserNicknameWordsLoader,
) {
    private val words = userNicknameWordsLoader.load()

    fun generate(): String = "${words.modifiers.random()}${words.animals.random()}"
}
