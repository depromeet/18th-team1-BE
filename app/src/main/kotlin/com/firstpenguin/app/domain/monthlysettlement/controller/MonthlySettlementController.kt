package com.firstpenguin.app.domain.monthlysettlement.controller

import com.firstpenguin.app.domain.auth.model.AuthenticatedUser
import com.firstpenguin.app.domain.monthlysettlement.dto.MonthlySettlementResponse
import com.firstpenguin.app.domain.monthlysettlement.usecase.MonthlySettlementUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/monthly-settlements")
@Tag(name = "월말 결산", description = "월말 결산 API")
class MonthlySettlementController(
    private val monthlySettlementUseCase: MonthlySettlementUseCase,
) {
    @Operation(
        summary = "월말 결산 조회 API",
        description = "사용자가 해당 월에 추천받은 문장 기준의 월말 결산을 조회한다.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @GetMapping
    fun getMonthlySettlement(
        @Parameter(hidden = true) @AuthenticationPrincipal authenticatedUser: AuthenticatedUser,
        @Parameter(description = "조회 연도", example = "2026") @RequestParam year: Int,
        @Parameter(description = "조회 월", example = "3") @RequestParam month: Int,
    ): ResponseEntity<MonthlySettlementResponse> =
        ResponseEntity.ok(
            monthlySettlementUseCase.getMonthlySettlement(
                userId = authenticatedUser.id,
                year = year,
                month = month,
            ),
        )

    @Operation(
        summary = "월말 결산 공유 조회 API",
        description = "공유 URL에서 토큰 없이 사용자 ID와 조회 월로 월말 결산을 조회한다.",
    )
    @GetMapping("/shared")
    fun getSharedMonthlySettlement(
        @Parameter(description = "조회 대상 사용자 ID", example = "1") @RequestParam userId: Long,
        @Parameter(description = "조회 연도", example = "2026") @RequestParam year: Int,
        @Parameter(description = "조회 월", example = "3") @RequestParam month: Int,
    ): ResponseEntity<MonthlySettlementResponse> =
        ResponseEntity.ok(
            monthlySettlementUseCase.getMonthlySettlement(
                userId = userId,
                year = year,
                month = month,
            ),
        )
}
