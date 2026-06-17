package com.firstpenguin.app.domain.recommendation.service

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

const val RECOMMENDATION_PREFETCH_EXECUTOR_NAME = "recommendationPrefetchExecutor"
const val RECOMMENDATION_ANALYSIS_EXECUTOR_NAME = "recommendationAnalysisExecutor"
const val RECOMMENDATION_EMBEDDING_EXECUTOR_NAME = "recommendationEmbeddingExecutor"

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

    @Bean(name = [RECOMMENDATION_EMBEDDING_EXECUTOR_NAME], destroyMethod = "shutdown")
    fun recommendationEmbeddingExecutor(): ExecutorService = Executors.newVirtualThreadPerTaskExecutor()

    @Bean(name = [RECOMMENDATION_ANALYSIS_EXECUTOR_NAME], destroyMethod = "shutdown")
    fun recommendationAnalysisExecutor(): ExecutorService = Executors.newVirtualThreadPerTaskExecutor()

    private companion object {
        const val CORE_POOL_SIZE = 4
        const val MAX_POOL_SIZE = 8
        const val QUEUE_CAPACITY = 100
        const val THREAD_NAME_PREFIX = "recommendation-prefetch-"
    }
}
