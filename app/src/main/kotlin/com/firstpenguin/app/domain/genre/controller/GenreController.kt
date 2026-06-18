package com.firstpenguin.app.domain.genre.controller

import com.firstpenguin.app.domain.genre.dto.GenreResponse
import com.firstpenguin.app.domain.genre.usecase.GenreUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/genres")
@Tag(name = "장르", description = "장르 기준 데이터 API")
class GenreController(
    private val genreUseCase: GenreUseCase,
) {
    @Operation(
        summary = "장르 목록 조회 API",
        description = "서버에 저장된 장르 목록을 정렬 순서대로 반환한다.",
    )
    @GetMapping
    fun getGenres(): List<GenreResponse> = genreUseCase.getGenres()
}
