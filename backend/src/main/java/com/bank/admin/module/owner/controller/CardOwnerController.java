package com.bank.admin.module.owner.controller;

import com.bank.admin.common.annotation.Log;
import com.bank.admin.common.enums.ActionTypeEnum;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.Result;
import com.bank.admin.module.owner.dto.CardOwnerQueryDTO;
import com.bank.admin.module.owner.dto.CardOwnerSaveDTO;
import com.bank.admin.module.owner.service.CardOwnerService;
import com.bank.admin.module.owner.vo.CardOwnerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 持卡人 Controller
 */
@Tag(name = "持卡人管理")
@RestController
@RequestMapping("/api/owners")
@RequiredArgsConstructor
public class CardOwnerController {

    private final CardOwnerService cardOwnerService;

    @Operation(summary = "分页查询持卡人")
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<PageResult<CardOwnerVO>> page(@Valid CardOwnerQueryDTO query) {
        return Result.success(cardOwnerService.page(query));
    }

    @Operation(summary = "持卡人详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<CardOwnerVO> detail(@PathVariable Long id) {
        return Result.success(cardOwnerService.detail(id));
    }

    @Operation(summary = "新增持卡人")
    @Log(module = "持卡人管理", type = ActionTypeEnum.INSERT, description = "新增持卡人")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> save(@Valid @RequestBody CardOwnerSaveDTO dto) {
        cardOwnerService.save(dto);
        return Result.success();
    }

    @Operation(summary = "编辑持卡人")
    @Log(module = "持卡人管理", type = ActionTypeEnum.UPDATE, description = "编辑持卡人")
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> update(@Valid @RequestBody CardOwnerSaveDTO dto) {
        cardOwnerService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除持卡人")
    @Log(module = "持卡人管理", type = ActionTypeEnum.DELETE, description = "删除持卡人[id=#id]")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        cardOwnerService.delete(id);
        return Result.success();
    }
}
