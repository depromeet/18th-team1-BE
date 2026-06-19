package com.firstpenguin.app.domain.monthlysettlement.controller

import com.firstpenguin.app.domain.auth.model.AuthenticatedUser
import com.firstpenguin.app.domain.monthlysettlement.dto.MonthlySettlementResponse
import com.firstpenguin.app.domain.monthlysettlement.usecase.MonthlySettlementUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import kotlin.test.assertEquals

class MonthlySettlementControllerTest {
    private lateinit var monthlySettlementUseCase: MonthlySettlementUseCase
    private lateinit var monthlySettlementController: MonthlySettlementController

    @BeforeEach
    fun setUp() {
        monthlySettlementUseCase = Mockito.mock(MonthlySettlementUseCase::class.java)
        monthlySettlementController = MonthlySettlementController(monthlySettlementUseCase)
    }

    @Test
    fun `인증 사용자 월말 결산을 조회한다`() {
        val response = response(USER_ID)
        Mockito
            .`when`(monthlySettlementUseCase.getMonthlySettlement(USER_ID, YEAR, MONTH))
            .thenReturn(response)

        val result =
            monthlySettlementController.getMonthlySettlement(
                authenticatedUser = AuthenticatedUser(USER_ID),
                year = YEAR,
                month = MONTH,
            )

        assertEquals(response, result.body)
        Mockito.verify(monthlySettlementUseCase).getMonthlySettlement(USER_ID, YEAR, MONTH)
    }

    @Test
    fun `토큰 없이 사용자 ID로 공유 월말 결산을 조회한다`() {
        val response = response(SHARED_USER_ID)
        Mockito
            .`when`(monthlySettlementUseCase.getMonthlySettlement(SHARED_USER_ID, YEAR, MONTH))
            .thenReturn(response)

        val result =
            monthlySettlementController.getSharedMonthlySettlement(
                userId = SHARED_USER_ID,
                year = YEAR,
                month = MONTH,
            )

        assertEquals(response, result.body)
        Mockito.verify(monthlySettlementUseCase).getMonthlySettlement(SHARED_USER_ID, YEAR, MONTH)
    }

    private fun response(userId: Long): MonthlySettlementResponse =
        MonthlySettlementResponse(
            year = YEAR,
            month = MONTH,
            sharedQuoteCount = userId.toInt(),
            mostFrequentGenre = null,
            monthlyBooks = emptyList(),
            emotionTags = emptyList(),
            recommendationMessage = null,
            monthlyBook = null,
        )

    private companion object {
        const val USER_ID = 10L
        const val SHARED_USER_ID = 20L
        const val YEAR = 2026
        const val MONTH = 3
    }
}
