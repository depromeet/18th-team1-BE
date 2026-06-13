package com.firstpenguin.app.domain.recommendation.model

import com.firstpenguin.app.domain.emotion.model.Tag

data class RecommendationInput(
    val userId: Long,
    val emotionRangeId: Long,
    val emotionTags: List<Tag>,
    val needTag: Tag?,
    val feelingText: String?,
    val diaryText: String?,
    val analysis: UserInputAnalysis?,
)
