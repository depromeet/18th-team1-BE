package com.firstpenguin.app.domain.discovery.service

import com.firstpenguin.app.domain.discovery.model.DiscoveryCursor
import com.firstpenguin.app.domain.discovery.model.DiscoveryGenre
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuote
import com.firstpenguin.app.domain.discovery.model.DiscoveryQuoteSearchCriteria
import com.firstpenguin.app.domain.discovery.repository.DiscoveryRepository
import org.springframework.stereotype.Service

@Service
class DiscoveryService(
    private val discoveryRepository: DiscoveryRepository,
) {
    fun getRecommendedQuotes(
        userId: Long,
        cursor: DiscoveryCursor?,
        genre: DiscoveryGenre?,
        limit: Int,
    ): List<DiscoveryQuote> =
        discoveryRepository.findRecommendedQuotes(
            userId = userId,
            cursor = cursor,
            genre = genre,
            limit = limit,
        )

    fun searchRecommendedQuotes(criteria: DiscoveryQuoteSearchCriteria): List<DiscoveryQuote> =
        discoveryRepository.searchRecommendedQuotes(criteria)
}
