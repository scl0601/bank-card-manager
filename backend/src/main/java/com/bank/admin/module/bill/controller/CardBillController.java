package com.bank.admin.module.bill.controller;

import com.bank.admin.common.annotation.Log;
import com.bank.admin.common.enums.ActionTypeEnum;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.Result;
import com.bank.admin.module.bill.dto.BillDetailSaveDTO;
import com.bank.admin.module.bill.dto.BatchUpdateTypeDTO;
import com.bank.admin.module.bill.dto.CardBillQueryDTO;
import com.bank.admin.module.bill.dto.CardBillSaveDTO;
import com.bank.admin.module.bill.service.BillDetailService;
import com.bank.admin.module.bill.service.CardBillService;
import com.bank.admin.module.bill.vo.BillDetailVO;
import com.bank.admin.module.bill.vo.CardBillOverviewVO;
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
import java.util.List;

/**
 * 账单 Controller（含明细管理、自动生成年账单、账单日/还款日联动）
 */
@Tag(name = "账单管理")
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class CardBillController {

    private final CardBillService cardBillService;
    private final BillDetailService billDetailService;

    @Operation(summary = "分页查询账单")
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<PageResult<CardBillVO>> page(@Valid CardBillQueryDTO query) {
        return Result.success(cardBillService.page(query));
    }

    @Operation(summary = "账单汇总")
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<CardBillOverviewVO> overview(@Valid CardBillQueryDTO query) {
        return Result.success(cardBillService.overview(query));
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

    @Operation(summary = "批量删除账单")
    @Log(module = "账单管理", type = ActionTypeEnum.DELETE, description = "批量删除账单")
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        cardBillService.batchDelete(ids);
        return Result.success();
    }

    @Operation(summary = "为信用卡自动生成指定年份 1-12 月账单")
    @PostMapping("/generate-annual/{cardId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> generateAnnual(
            @PathVariable Long cardId,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Integer year,
            @RequestParam Integer billDay,
            @RequestParam Integer repayDay,
            @RequestParam(required = false) BigDecimal feeRate) {
        cardBillService.generateAnnualBills(cardId, ownerId, billDay, repayDay, feeRate, year);
        return Result.success();
    }

    @Operation(summary = "修改账单日/还款日后，同步更新当前月份到当年12月")
    @PatchMapping("/{cardId}/sync-schedule")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> syncSchedule(
            @PathVariable Long cardId,
            @RequestParam String fromBillMonth,
            @RequestParam(required = false) Integer newBillDay,
            @RequestParam(required = false) Integer newRepayDay) {
        cardBillService.syncScheduleFromMonth(cardId, fromBillMonth, newBillDay, newRepayDay, null, null);
        return Result.success();
    }

    @Operation(summary = "查询某账单的明细列表")
    @GetMapping("/{billId}/details")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<List<BillDetailVO>> listDetails(@PathVariable Long billId) {
        return Result.success(billDetailService.listByBillId(billId));
    }

    @Operation(summary = "新增明细")
    @Log(module = "账单明细", type = ActionTypeEnum.INSERT, description = "新增账单明细")
    @PostMapping("/details")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> saveDetail(@Valid @RequestBody BillDetailSaveDTO dto) {
        billDetailService.save(dto);
        return Result.success();
    }

    @Operation(summary = "编辑明细")
    @Log(module = "账单明细", type = ActionTypeEnum.UPDATE, description = "编辑账单明细")
    @PutMapping("/details")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> updateDetail(@Valid @RequestBody BillDetailSaveDTO dto) {
        billDetailService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除明细")
    @Log(module = "账单明细", type = ActionTypeEnum.DELETE, description = "删除账单明细[id=#id]")
    @DeleteMapping("/details/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> deleteDetail(@PathVariable Long id) {
        billDetailService.delete(id);
        return Result.success();
    }

    @Operation(summary = "批量删除明细")
    @Log(module = "账单明细", type = ActionTypeEnum.DELETE, description = "批量删除账单明细")
    @DeleteMapping("/details/batch")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> batchDeleteDetails(@RequestBody List<Long> ids) {
        billDetailService.batchDelete(ids);
        return Result.success();
    }

    @Operation(summary = "批量修改明细收支状态")
    @Log(module = "账单明细", type = ActionTypeEnum.UPDATE, description = "批量修改明细收支状态")
    @PatchMapping("/details/batch-type")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> batchUpdateType(@Valid @RequestBody BatchUpdateTypeDTO dto) {
        billDetailService.batchUpdateType(dto.getIds(), dto.getDetailType());
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
