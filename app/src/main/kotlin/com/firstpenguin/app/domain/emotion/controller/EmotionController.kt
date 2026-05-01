package com.firstpenguin.app.domain.emotion.controller

import com.firstpenguin.app.domain.emotion.dto.TagResponse
import com.firstpenguin.app.domain.emotion.dto.TagSelectRequest
import com.firstpenguin.app.domain.emotion.dto.TagSelectResponse
import com.firstpenguin.app.domain.emotion.facade.EmotionFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private const val MIN_EMOTION_SCORE = 0L
private const val MAX_EMOTION_SCORE = 100L

@RestController
@RequestMapping("/emotions")
@Tag(name = "EMOTION", description = "감정 API")
class EmotionController(
    private val emotionFacade: EmotionFacade
) {

    @Operation(
        summary = "감정 점수 기반 감정 태그 목록 조회 API",
        description = "사용자의 감정 점수를 기준으로 감정 범위를 찾고, 해당 감정 범위에 속한 태그 목록을 반환한다."
    )
    @GetMapping("/emotion-tags")
    fun getEmotionTags(
        @RequestParam @Min(MIN_EMOTION_SCORE) @Max(MAX_EMOTION_SCORE) value: Int
    ): ResponseEntity<TagResponse> {
        return ResponseEntity.ok(emotionFacade.getEmotionTags(value))
    }

    @Operation(
        summary = "톤 태그 목록 조회 API",
        description = "톤 태그 목록을 반환한다."
    )
    @GetMapping("/tone-tags")
    fun getToneTags(): ResponseEntity<TagResponse> {
        return ResponseEntity.ok(emotionFacade.getToneTags())
    }

    @Operation(
        summary = "감정, 톤 태그 선택 API",
        description = "사용자가 선택한 감정 태그와 톤 태그를 검증하고, 추천 요청에 사용할 선택 태그 정보를 반환한다."
    )
    @PostMapping("/tag-selections")
    fun selectEmotionTags(
        @RequestBody request: TagSelectRequest,
    ): ResponseEntity<TagSelectResponse> {
        return ResponseEntity.ok(emotionFacade.selectEmotionTags(request))
    }
}
