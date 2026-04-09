package com.bank.admin.module.card.controller;

import com.bank.admin.common.annotation.Log;
import com.bank.admin.common.enums.ActionTypeEnum;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.Result;
import com.bank.admin.module.card.dto.BankCardQueryDTO;
import com.bank.admin.module.card.dto.BankCardSaveDTO;
import com.bank.admin.module.card.service.BankCardService;
import com.bank.admin.module.card.vo.BankCardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 银行卡 Controller
 */
@Tag(name = "银行卡管理")
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class BankCardController {

    private final BankCardService bankCardService;

    @Operation(summary = "获取所有正常银行卡（用于下拉选择）")
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<List<BankCardVO>> list() {
        return Result.success(bankCardService.listAll());
    }

    @Operation(summary = "分页查询银行卡")
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<PageResult<BankCardVO>> page(@Valid BankCardQueryDTO query) {
        return Result.success(bankCardService.page(query));
    }

    @Operation(summary = "银行卡详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<BankCardVO> detail(@PathVariable Long id) {
        return Result.success(bankCardService.detail(id));
    }

    @Operation(summary = "新增银行卡")
    @Log(module = "银行卡管理", type = ActionTypeEnum.INSERT, description = "新增银行卡")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> save(@Valid @RequestBody BankCardSaveDTO dto) {
        bankCardService.save(dto);
        return Result.success();
    }

    @Operation(summary = "编辑银行卡")
    @Log(module = "银行卡管理", type = ActionTypeEnum.UPDATE, description = "编辑银行卡")
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> update(@Valid @RequestBody BankCardSaveDTO dto) {
        bankCardService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除银行卡")
    @Log(module = "银行卡管理", type = ActionTypeEnum.DELETE, description = "删除银行卡[id=#id]")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        bankCardService.delete(id);
        return Result.success();
    }
}
