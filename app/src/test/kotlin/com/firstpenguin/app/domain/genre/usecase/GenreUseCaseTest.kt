package com.firstpenguin.app.domain.genre.usecase

import com.firstpenguin.app.domain.genre.model.Genre
import com.firstpenguin.app.domain.genre.service.GenreService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import kotlin.test.assertEquals

class GenreUseCaseTest {
    private lateinit var genreService: GenreService
    private lateinit var genreUseCase: GenreUseCase

    @BeforeEach
    fun setUp() {
        genreService = Mockito.mock(GenreService::class.java)
        genreUseCase = GenreUseCase(genreService)
    }

    @Test
    fun `장르 목록을 응답 형식으로 변환한다`() {
        Mockito
            .`when`(genreService.getGenres())
            .thenReturn(listOf(Genre(GENRE_ID, GENRE_LABEL, SORT_ORDER)))

        val response = genreUseCase.getGenres()

        assertEquals(1, response.size)
        assertEquals(GENRE_ID, response.first().genreId)
        assertEquals(GENRE_LABEL, response.first().label)
    }

    private companion object {
        const val GENRE_ID = 1L
        const val GENRE_LABEL = "일반문학"
        const val SORT_ORDER = 1
    }
}
