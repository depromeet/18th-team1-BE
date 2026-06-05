package com.firstpenguin.app.domain.discovery.service

import com.firstpenguin.app.domain.discovery.model.DiscoveryQuote
import com.firstpenguin.app.domain.discovery.repository.DiscoveryRepository
import org.springframework.stereotype.Service

@Service
class DiscoveryService(
    private val discoveryRepository: DiscoveryRepository,
) {
    fun getRandomQuotes(
        userId: Long,
        limit: Int,
    ): List<DiscoveryQuote> = discoveryRepository.findRandomRecommendedQuotes(userId, limit)
}
