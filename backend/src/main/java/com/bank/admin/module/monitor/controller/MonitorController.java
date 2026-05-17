package com.bank.admin.module.monitor.controller;

import com.bank.admin.common.result.Result;
import com.bank.admin.module.monitor.service.MonitorService;
import com.bank.admin.module.monitor.vo.MonitorTodayVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "监控列表")
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    @Operation(summary = "获取指定日期监控时间线")
    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN','MONITOR')")
    public Result<MonitorTodayVO> today(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        return Result.success(monitorService.getToday(date));
    }
}
