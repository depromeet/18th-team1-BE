package com.firstpenguin.app.domain.recommendation.service

import org.slf4j.Logger

private const val RECOMMENDATION_TIMING_LOG_PREFIX = "[recommendation-timing]"
private const val NANOSECONDS_PER_MILLISECOND = 1_000_000

internal fun <T> Logger.measureRecommendationStep(
    step: String,
    detail: () -> String = { "" },
    block: () -> T,
): T {
    val startedAt = System.nanoTime()

    return try {
        block()
    } finally {
        logRecommendationTiming(step, startedAt, detail.safelyBuild())
    }
}

private fun (() -> String).safelyBuild(): String = runCatching { this() }.getOrDefault("detail=unavailable")

private fun Logger.logRecommendationTiming(
    step: String,
    startedAt: Long,
    detail: String,
) {
    val elapsedMs = (System.nanoTime() - startedAt) / NANOSECONDS_PER_MILLISECOND
    if (detail.isBlank()) {
        info("{} step={} elapsedMs={}", RECOMMENDATION_TIMING_LOG_PREFIX, step, elapsedMs)
        return
    }

    info("{} step={} elapsedMs={} {}", RECOMMENDATION_TIMING_LOG_PREFIX, step, elapsedMs, detail)
}
