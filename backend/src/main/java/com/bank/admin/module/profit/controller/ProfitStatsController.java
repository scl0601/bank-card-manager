package com.bank.admin.module.profit.controller;

import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.Result;
import com.bank.admin.module.profit.dto.ProfitQueryDTO;
import com.bank.admin.module.profit.service.ProfitStatsService;
import com.bank.admin.module.profit.vo.CardProfitVO;
import com.bank.admin.module.profit.vo.MonthlyProfitVO;
import com.bank.admin.module.profit.vo.ProfitOverviewVO;
import com.bank.admin.module.profit.vo.UserProfitVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Profit stats")
@RestController
@RequestMapping("/api/profits")
@RequiredArgsConstructor
public class ProfitStatsController {

    private final ProfitStatsService profitStatsService;

    @Operation(summary = "Profit overview")
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<ProfitOverviewVO> overview(@Valid ProfitQueryDTO query) {
        return Result.success(profitStatsService.getOverview(query));
    }

    @Operation(summary = "User profit page")
    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<PageResult<UserProfitVO>> userPage(@Valid ProfitQueryDTO query) {
        return Result.success(profitStatsService.pageUserProfit(query));
    }

    @Operation(summary = "Card profit page")
    @GetMapping("/cards")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<PageResult<CardProfitVO>> cardPage(@Valid ProfitQueryDTO query) {
        return Result.success(profitStatsService.pageCardProfit(query));
    }

    @Operation(summary = "User monthly profit list")
    @GetMapping("/user-months")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<List<UserProfitVO>> userMonths(@Valid ProfitQueryDTO query) {
        return Result.success(profitStatsService.listUserMonthProfit(query));
    }

    @Operation(summary = "Card monthly profit list")
    @GetMapping("/card-months")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<List<CardProfitVO>> cardMonths(@Valid ProfitQueryDTO query) {
        return Result.success(profitStatsService.listCardMonthProfit(query));
    }

    @Operation(summary = "Monthly profit list")
    @GetMapping("/months")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<List<MonthlyProfitVO>> months(@Valid ProfitQueryDTO query) {
        return Result.success(profitStatsService.listMonthlyProfit(query));
    }
}
