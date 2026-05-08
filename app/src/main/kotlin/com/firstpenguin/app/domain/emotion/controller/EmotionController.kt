package com.firstpenguin.app.domain.emotion.controller

import com.firstpenguin.app.domain.emotion.dto.TagResponse
import com.firstpenguin.app.domain.emotion.useCase.EmotionUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private const val MIN_EMOTION_VALUE = 1L
private const val MAX_EMOTION_VALUE = 9L

@RestController
@RequestMapping("/emotions")
@Tag(name = "감정", description = "감정 선택값 및 감정/톤 태그 API")
class EmotionController(
    private val emotionUseCase: EmotionUseCase,
) {
    @Operation(
        summary = "감정 점수 기반 감정 태그 목록 조회 API",
        description = "사용자의 감정 점수를 기준으로 감정 범위를 찾고, 해당 감정 범위에 속한 태그 목록을 반환한다.",
    )
    @GetMapping("/emotion-tags")
    @Validated
    fun getEmotionTags(
        @RequestParam @Min(MIN_EMOTION_VALUE) @Max(MAX_EMOTION_VALUE) value: Int,
    ): ResponseEntity<TagResponse> = ResponseEntity.ok(emotionUseCase.getEmotionTags(value))

    @Operation(
        summary = "톤 태그 목록 조회 API",
        description = "톤 태그 목록을 반환한다.",
    )
    @GetMapping("/tone-tags")
    fun getToneTags(): ResponseEntity<TagResponse> = ResponseEntity.ok(emotionUseCase.getToneTags())
}
