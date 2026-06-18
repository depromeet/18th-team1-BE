package com.firstpenguin.app.domain.genre.usecase

import com.firstpenguin.app.domain.genre.dto.GenreResponse
import com.firstpenguin.app.domain.genre.service.GenreService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GenreUseCase(
    private val genreService: GenreService,
) {
    @Transactional(readOnly = true)
    fun getGenres(): List<GenreResponse> =
        genreService
            .getGenres()
            .map(GenreResponse::from)
}
