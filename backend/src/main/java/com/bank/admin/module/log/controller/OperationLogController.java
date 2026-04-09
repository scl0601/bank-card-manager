package com.bank.admin.module.log.controller;

import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.Result;
import com.bank.admin.module.log.dto.OperationLogQueryDTO;
import com.bank.admin.module.log.entity.OperationLog;
import com.bank.admin.module.log.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 操作日志 Controller
 */
@Tag(name = "系统日志")
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @Operation(summary = "分页查询操作日志")
    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<OperationLog>> page(@Valid OperationLogQueryDTO query) {
        return Result.success(operationLogService.page(query));
    }

    @Operation(summary = "导出操作日志Excel")
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void export(OperationLogQueryDTO query, HttpServletResponse response) throws Exception {
        String fileName = URLEncoder.encode("操作日志", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName + ".xlsx");
        operationLogService.exportExcel(query, response.getOutputStream());
    }
}
