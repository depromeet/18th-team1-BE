package com.firstpenguin.app.domain.home.controller

import com.firstpenguin.app.domain.home.userCase.HomeUserCase
import com.firstpenguin.app.domain.quote.dto.QuoteResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/home")
@Tag(name = "홈", description = "홈 API")
class HomeController(
    private val homeUserCase: HomeUserCase,
) {
    @Operation(
        summary = "랜덤 추천 문구 API",
        description = "홈화면 랜덤 추천 문구를 정보를 반환한다.",
    )
    @GetMapping("/random")
    fun getRandomQuote(): ResponseEntity<QuoteResponse> = ResponseEntity.ok(homeUserCase.getRandomQuote())
}
