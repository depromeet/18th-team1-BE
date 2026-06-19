package com.firstpenguin.app.domain.monthlysettlement.usecase

import com.firstpenguin.app.domain.monthlysettlement.dto.MonthlySettlementResponse
import com.firstpenguin.app.domain.monthlysettlement.service.MonthlySettlementService
import com.firstpenguin.app.global.exception.CustomException
import com.firstpenguin.app.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.time.ZoneId

@Service
class MonthlySettlementUseCase(
    private val monthlySettlementService: MonthlySettlementService,
) {
    @Transactional(readOnly = true)
    fun getMonthlySettlement(
        userId: Long,
        year: Int,
        month: Int,
    ): MonthlySettlementResponse {
        val yearMonth = validateYearMonth(year, month)
        val snapshot =
            monthlySettlementService.createSnapshot(userId, yearMonth)
                ?: return MonthlySettlementResponse.empty(
                    year = yearMonth.year,
                    month = yearMonth.monthValue,
                )

        return MonthlySettlementResponse.from(snapshot)
    }

    private fun validateYearMonth(
        year: Int,
        month: Int,
    ): YearMonth {
        if (year < MIN_YEAR || month !in MIN_MONTH..MAX_MONTH) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }

        val requestedMonth = YearMonth.of(year, month)
        if (!requestedMonth.isAfter(currentMonth())) return requestedMonth

        throw CustomException(ErrorCode.MONTHLY_SETTLEMENT_NOT_AVAILABLE)
    }

    private fun currentMonth(): YearMonth = YearMonth.now(ZoneId.of("Asia/Seoul"))

    private companion object {
        const val MIN_YEAR = 1
        const val MIN_MONTH = 1
        const val MAX_MONTH = 12
    }
}
