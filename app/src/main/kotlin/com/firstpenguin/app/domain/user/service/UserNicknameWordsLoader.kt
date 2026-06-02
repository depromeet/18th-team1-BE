package com.firstpenguin.app.domain.user.service

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.util.Properties

@Component
class UserNicknameWordsLoader {
    fun load(): UserNicknameWords {
        val properties = loadProperties()
        return UserNicknameWords(
            modifiers = properties.getRequiredList(MODIFIERS_KEY),
            animals = properties.getRequiredList(ANIMALS_KEY),
        ).also(::validate)
    }

    private fun loadProperties(): Properties {
        val factory = YamlPropertiesFactoryBean()
        factory.setResources(ClassPathResource(NICKNAME_WORDS_PATH))
        return factory.`object` ?: error("Nickname words file is empty")
    }

    private fun Properties.getRequiredList(key: String): List<String> =
        indexedValues(key).also {
            require(it.isNotEmpty()) { "$key must not be empty" }
        }

    private fun Properties.indexedValues(key: String): List<String> =
        generateSequence(0) { index -> index + 1 }
            .map { index -> getProperty("$key[$index]") }
            .takeWhile { value -> value != null }
            .filterNotNull()
            .toList()

    private fun validate(words: UserNicknameWords) {
        validateWords(MODIFIERS_KEY, words.modifiers)
        validateWords(ANIMALS_KEY, words.animals)
        validateNicknameLength(words)
    }

    private fun validateWords(
        key: String,
        words: List<String>,
    ) {
        require(words.none { it.isBlank() }) { "$key must not contain blank word" }
        require(words.none { word -> word.any { it.isWhitespace() } }) { "$key must not contain whitespace" }
        require(words.size == words.toSet().size) { "$key must not contain duplicate word" }
    }

    private fun validateNicknameLength(words: UserNicknameWords) {
        val invalidNickname =
            words.modifiers
                .asSequence()
                .flatMap { modifier -> words.animals.asSequence().map { animal -> "$modifier$animal" } }
                .firstOrNull { it.length > MAX_NICKNAME_LENGTH }
        require(invalidNickname == null) { "$invalidNickname exceeds nickname length limit" }
    }

    private companion object {
        const val NICKNAME_WORDS_PATH = "nickname-words.yml"
        const val MODIFIERS_KEY = "modifiers"
        const val ANIMALS_KEY = "animals"
        const val MAX_NICKNAME_LENGTH = 15
    }
}
