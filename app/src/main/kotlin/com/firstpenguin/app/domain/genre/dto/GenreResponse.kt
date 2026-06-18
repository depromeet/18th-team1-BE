package com.firstpenguin.app.domain.genre.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.firstpenguin.app.domain.genre.model.Genre
import io.swagger.v3.oas.annotations.media.Schema

data class GenreResponse(
    @get:JsonProperty("genre_id")
    @field:Schema(description = "장르 ID", example = "1")
    val genreId: Long,
    @field:Schema(description = "장르 표시명", example = "일반문학")
    val label: String,
) {
    companion object {
        fun from(genre: Genre): GenreResponse =
            GenreResponse(
                genreId = genre.id,
                label = genre.label,
            )
    }
}
