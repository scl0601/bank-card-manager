package com.bank.admin.module.transaction.controller;

import com.bank.admin.common.annotation.Log;
import com.bank.admin.common.enums.ActionTypeEnum;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.Result;
import com.bank.admin.module.transaction.dto.CardTransactionQueryDTO;
import com.bank.admin.module.transaction.dto.CardTransactionSaveDTO;
import com.bank.admin.module.transaction.service.CardTransactionService;
import com.bank.admin.module.transaction.vo.CardTransactionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 流水 Controller
 */
@Tag(name = "流水管理")
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class CardTransactionController {

    private final CardTransactionService cardTransactionService;

    @Operation(summary = "分页查询流水")
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<PageResult<CardTransactionVO>> page(@Valid CardTransactionQueryDTO query) {
        return Result.success(cardTransactionService.page(query));
    }

    @Operation(summary = "新增流水")
    @Log(module = "流水管理", type = ActionTypeEnum.INSERT, description = "新增流水记录")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> save(@Valid @RequestBody CardTransactionSaveDTO dto) {
        cardTransactionService.save(dto);
        return Result.success();
    }

    @Operation(summary = "编辑流水")
    @Log(module = "流水管理", type = ActionTypeEnum.UPDATE, description = "编辑流水记录")
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> update(@Valid @RequestBody CardTransactionSaveDTO dto) {
        cardTransactionService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除流水")
    @Log(module = "流水管理", type = ActionTypeEnum.DELETE, description = "删除流水记录[id=#id]")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        cardTransactionService.delete(id);
        return Result.success();
    }

    @Operation(summary = "导出流水Excel")
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public void export(CardTransactionQueryDTO query, HttpServletResponse response) throws Exception {
        String fileName = URLEncoder.encode("流水记录", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName + ".xlsx");
        cardTransactionService.exportExcel(query, response.getOutputStream());
    }

    @Operation(summary = "批量删除流水")
    @Log(module = "流水管理", type = ActionTypeEnum.DELETE, description = "批量删除流水")
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        cardTransactionService.batchDelete(ids);
        return Result.success();
    }
}
