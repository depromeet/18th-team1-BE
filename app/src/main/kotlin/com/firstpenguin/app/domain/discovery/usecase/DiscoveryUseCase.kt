package com.firstpenguin.app.domain.discovery.usecase

import com.firstpenguin.app.domain.discovery.dto.DiscoveryQuoteResponse
import com.firstpenguin.app.domain.discovery.dto.DiscoveryQuotesResponse
import com.firstpenguin.app.domain.discovery.service.DiscoveryService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val DISCOVERY_QUOTE_COUNT = 10

@Service
class DiscoveryUseCase(
    private val discoveryService: DiscoveryService,
) {
    @Transactional(readOnly = true)
    fun getDiscoveryQuotes(userId: Long): DiscoveryQuotesResponse {
        val quotes = discoveryService.getRandomQuotes(userId, DISCOVERY_QUOTE_COUNT)

        return DiscoveryQuotesResponse(
            quotes = quotes.map(DiscoveryQuoteResponse::from),
        )
    }
}
