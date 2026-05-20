package com.bank.admin.module.special.controller;

import com.bank.admin.common.annotation.Log;
import com.bank.admin.common.enums.ActionTypeEnum;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.Result;
import com.bank.admin.module.special.dto.SpecialBillAfterYearDeleteDTO;
import com.bank.admin.module.special.dto.SpecialBillBatchDeleteDTO;
import com.bank.admin.module.special.dto.SpecialBillQueryDTO;
import com.bank.admin.module.special.dto.SpecialBillUpdateDTO;
import com.bank.admin.module.special.dto.SpecialCardSaveDTO;
import com.bank.admin.module.special.dto.SpecialProfitExtraFeeUpdateDTO;
import com.bank.admin.module.special.dto.SpecialProfitQueryDTO;
import com.bank.admin.module.special.dto.SpecialUserConfigSaveDTO;
import com.bank.admin.module.special.service.SpecialChannelService;
import com.bank.admin.module.special.vo.SpecialBillImportResultVO;
import com.bank.admin.module.special.vo.SpecialBillVO;
import com.bank.admin.module.special.vo.SpecialCardVO;
import com.bank.admin.module.special.vo.SpecialConfigVO;
import com.bank.admin.module.special.vo.SpecialProfitStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "特殊通道")
@RestController
@RequestMapping("/api/special")
@RequiredArgsConstructor
public class SpecialChannelController {

    private final SpecialChannelService specialChannelService;

    @Operation(summary = "获取特殊用户配置")
    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<SpecialConfigVO> getConfig() {
        return Result.success(specialChannelService.getConfig());
    }

    @Operation(summary = "保存特殊用户配置")
    @Log(module = "特殊通道", type = ActionTypeEnum.UPDATE, description = "保存特殊用户配置")
    @PutMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<SpecialConfigVO> saveConfig(@Valid @RequestBody SpecialUserConfigSaveDTO dto) {
        return Result.success(specialChannelService.saveConfig(dto));
    }

    @Operation(summary = "查询特殊银行卡")
    @GetMapping("/cards")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<List<SpecialCardVO>> listCards() {
        return Result.success(specialChannelService.listCards());
    }

    @Operation(summary = "新增特殊银行卡，并自动生成基础年度账单")
    @Log(module = "特殊卡务", type = ActionTypeEnum.INSERT, description = "新增特殊银行卡")
    @PostMapping("/cards")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Long> saveCard(@Valid @RequestBody SpecialCardSaveDTO dto) {
        return Result.success(specialChannelService.saveCard(dto));
    }

    @Operation(summary = "编辑特殊银行卡")
    @Log(module = "特殊卡务", type = ActionTypeEnum.UPDATE, description = "编辑特殊银行卡")
    @PutMapping("/cards")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> updateCard(@Valid @RequestBody SpecialCardSaveDTO dto) {
        specialChannelService.updateCard(dto);
        return Result.success();
    }

    @Operation(summary = "删除特殊银行卡")
    @Log(module = "特殊卡务", type = ActionTypeEnum.DELETE, description = "删除特殊银行卡[id=#id]")
    @DeleteMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteCard(@PathVariable Long id) {
        specialChannelService.deleteCard(id);
        return Result.success();
    }

    @Operation(summary = "分页查询特殊账单")
    @GetMapping("/bills/page")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<PageResult<SpecialBillVO>> pageBills(@Valid SpecialBillQueryDTO query) {
        return Result.success(specialChannelService.pageBills(query));
    }

    @Operation(summary = "编辑特殊账单")
    @Log(module = "特殊账单", type = ActionTypeEnum.UPDATE, description = "编辑特殊账单")
    @PutMapping("/bills")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> updateBill(@Valid @RequestBody SpecialBillUpdateDTO dto) {
        specialChannelService.updateBill(dto);
        return Result.success();
    }

    @Operation(summary = "批量编辑特殊账单")
    @Log(module = "特殊账单", type = ActionTypeEnum.UPDATE, description = "批量编辑特殊账单")
    @PutMapping("/bills/batch")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> batchUpdateBills(@Valid @RequestBody List<SpecialBillUpdateDTO> dtos) {
        specialChannelService.batchUpdateBills(dtos);
        return Result.success();
    }

    @Operation(summary = "导入特殊账单Excel")
    @Log(module = "特殊账单", type = ActionTypeEnum.IMPORT, description = "导入特殊账单Excel")
    @PostMapping("/bills/import")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<SpecialBillImportResultVO> importBills(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "year", required = false) Integer year) {
        return Result.success(specialChannelService.importBills(file, year));
    }

    @Operation(summary = "按银行卡批量删除指定年份之前的特殊账单")
    @Log(module = "特殊账单", type = ActionTypeEnum.DELETE, description = "批量删除指定年份之前的特殊账单")
    @DeleteMapping("/bills/before-year")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Integer> deleteBillsBeforeYear(@Valid @RequestBody SpecialBillBatchDeleteDTO dto) {
        return Result.success(specialChannelService.deleteBillsBeforeYear(dto));
    }

    @Operation(summary = "按银行卡批量删除指定年份之后的特殊账单")
    @Log(module = "特殊账单", type = ActionTypeEnum.DELETE, description = "批量删除指定年份之后的特殊账单")
    @DeleteMapping("/bills/after-year")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Integer> deleteBillsAfterYear(@Valid @RequestBody SpecialBillAfterYearDeleteDTO dto) {
        return Result.success(specialChannelService.deleteBillsAfterYear(dto));
    }

    @Operation(summary = "批量删除特殊账单")
    @Log(module = "特殊账单", type = ActionTypeEnum.DELETE, description = "批量删除特殊账单")
    @DeleteMapping("/bills/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> batchDeleteBills(@RequestBody List<Long> ids) {
        specialChannelService.batchDeleteBills(ids);
        return Result.success();
    }

    @Operation(summary = "特殊收益统计")
    @GetMapping("/profit/stats")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<SpecialProfitStatsVO> stats(@Valid SpecialProfitQueryDTO query) {
        return Result.success(specialChannelService.stats(query));
    }

    @Operation(summary = "编辑特殊收益额外费用")
    @Log(module = "特殊收益", type = ActionTypeEnum.UPDATE, description = "编辑特殊收益额外费用")
    @PutMapping("/profit/extra-fees")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> updateProfitExtraFees(@Valid @RequestBody SpecialProfitExtraFeeUpdateDTO dto) {
        specialChannelService.updateProfitExtraFees(dto);
        return Result.success();
    }

    @Operation(summary = "批量编辑特殊收益额外费用")
    @Log(module = "特殊收益", type = ActionTypeEnum.UPDATE, description = "批量编辑特殊收益额外费用")
    @PutMapping("/profit/extra-fees/batch")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> batchUpdateProfitExtraFees(@Valid @RequestBody List<SpecialProfitExtraFeeUpdateDTO> dtos) {
        specialChannelService.batchUpdateProfitExtraFees(dtos);
        return Result.success();
    }
}
