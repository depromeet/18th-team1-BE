package com.firstpenguin.app.domain.recommendation.service

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

const val RECOMMENDATION_PREFETCH_EXECUTOR_NAME = "recommendationPrefetchExecutor"

@Configuration
class RecommendationExecutorConfig {
    @Bean(name = [RECOMMENDATION_PREFETCH_EXECUTOR_NAME])
    fun recommendationPrefetchExecutor(): Executor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = CORE_POOL_SIZE
            maxPoolSize = MAX_POOL_SIZE
            queueCapacity = QUEUE_CAPACITY
            setThreadNamePrefix(THREAD_NAME_PREFIX)
            initialize()
        }

    private companion object {
        const val CORE_POOL_SIZE = 4
        const val MAX_POOL_SIZE = 8
        const val QUEUE_CAPACITY = 100
        const val THREAD_NAME_PREFIX = "recommendation-prefetch-"
    }
}
