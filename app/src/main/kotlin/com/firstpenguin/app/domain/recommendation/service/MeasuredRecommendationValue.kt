package com.firstpenguin.app.domain.recommendation.service

private const val NANOSECONDS_PER_MILLISECOND = 1_000_000

internal data class MeasuredRecommendationValue<T>(
    val value: T,
    val elapsedMs: Long,
)

internal fun <T> measureRecommendationElapsed(block: () -> T): MeasuredRecommendationValue<T> {
    val startedAt = System.nanoTime()
    val value = block()
    val elapsedMs = (System.nanoTime() - startedAt) / NANOSECONDS_PER_MILLISECOND

    return MeasuredRecommendationValue(
        value = value,
        elapsedMs = elapsedMs,
    )
}
