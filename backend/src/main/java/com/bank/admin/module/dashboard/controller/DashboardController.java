package com.bank.admin.module.dashboard.controller;

import com.bank.admin.common.result.Result;
import com.bank.admin.module.dashboard.service.DashboardService;
import com.bank.admin.module.dashboard.vo.DashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页看板 Controller
 */
@Tag(name = "首页看板")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "获取看板统计数据")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<DashboardVO> stats() {
        return Result.success(dashboardService.getStats());
    }
}
