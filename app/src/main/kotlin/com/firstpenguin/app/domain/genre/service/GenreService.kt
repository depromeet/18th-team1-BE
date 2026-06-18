package com.firstpenguin.app.domain.genre.service

import com.firstpenguin.app.domain.genre.model.Genre
import com.firstpenguin.app.domain.genre.repository.GenreRepository
import org.springframework.stereotype.Service

@Service
class GenreService(
    private val genreRepository: GenreRepository,
) {
    fun getGenres(): List<Genre> = genreRepository.findAll()
}
