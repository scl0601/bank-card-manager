package com.bank.admin.module.bill.controller;

import com.bank.admin.common.annotation.Log;
import com.bank.admin.common.enums.ActionTypeEnum;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.Result;
import com.bank.admin.module.bill.dto.CardBillQueryDTO;
import com.bank.admin.module.bill.dto.CardBillSaveDTO;
import com.bank.admin.module.bill.service.CardBillService;
import com.bank.admin.module.bill.vo.CardBillVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 账单 Controller
 */
@Tag(name = "账单管理")
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class CardBillController {

    private final CardBillService cardBillService;

    @Operation(summary = "分页查询账单")
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<PageResult<CardBillVO>> page(@Valid CardBillQueryDTO query) {
        return Result.success(cardBillService.page(query));
    }

    @Operation(summary = "账单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<CardBillVO> detail(@PathVariable Long id) {
        return Result.success(cardBillService.detail(id));
    }

    @Operation(summary = "新增账单")
    @Log(module = "账单管理", type = ActionTypeEnum.INSERT, description = "新增账单")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> save(@Valid @RequestBody CardBillSaveDTO dto) {
        cardBillService.save(dto);
        return Result.success();
    }

    @Operation(summary = "编辑账单")
    @Log(module = "账单管理", type = ActionTypeEnum.UPDATE, description = "编辑账单")
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> update(@Valid @RequestBody CardBillSaveDTO dto) {
        cardBillService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除账单")
    @Log(module = "账单管理", type = ActionTypeEnum.DELETE, description = "删除账单[id=#id]")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        cardBillService.delete(id);
        return Result.success();
    }

    @Operation(summary = "标记还款")
    @Log(module = "账单管理", type = ActionTypeEnum.OTHER, description = "标记账单已还款[id=#id]")
    @PatchMapping("/{id}/repay")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> repay(
            @PathVariable Long id,
            @Parameter(description = "实际还款金额") @RequestParam BigDecimal actualPayAmount,
            @Parameter(description = "还款日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate actualPayDate) {
        cardBillService.markRepaid(id, actualPayAmount, actualPayDate);
        return Result.success();
    }

    @Operation(summary = "导出账单Excel")
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public void export(CardBillQueryDTO query, HttpServletResponse response) throws Exception {
        String fileName = URLEncoder.encode("账单记录", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName + ".xlsx");
        cardBillService.exportExcel(query, response.getOutputStream());
    }
}
