package com.firstpenguin.app.emotion.controller

import com.firstpenguin.app.emotion.dto.TagResponse
import com.firstpenguin.app.emotion.facade.EmotionFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/emotions")
@Tag(name = "EMOTION", description = "감정 API")
class EmotionController(
    private val emotionFacade: EmotionFacade
) {

    @Operation(
        summary = "감정 점수 기반 태그 목록 조회 API",
        description = "용자의 감정 점수를 기준으로 감정 범위를 찾고, 해당 감정 범위에 속한 태그 목록을 반환한다"
    )
    @GetMapping("/tags")
    fun getEmotionTags(
        @RequestParam @Min(0) @Max(100) value: Int
    ): ResponseEntity<TagResponse> {
        return ResponseEntity.ok(emotionFacade.getEmotionTags(value))
    }
}
