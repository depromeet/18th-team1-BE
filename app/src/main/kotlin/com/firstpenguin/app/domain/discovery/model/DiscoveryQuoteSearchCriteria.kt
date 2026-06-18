package com.firstpenguin.app.domain.discovery.model

data class DiscoveryQuoteSearchCriteria(
    val userId: Long,
    val query: String,
    val sort: DiscoveryQuoteSearchSort,
    val cursor: DiscoveryQuoteSearchCursor?,
    val genreId: Long?,
    val limit: Int,
)
