package com.bank.admin.module.card.controller;

import com.bank.admin.common.annotation.Log;
import com.bank.admin.common.enums.ActionTypeEnum;
import com.bank.admin.common.result.PageResult;
import com.bank.admin.common.result.Result;
import com.bank.admin.module.card.dto.CardUserQueryDTO;
import com.bank.admin.module.card.dto.CardUserSaveDTO;
import com.bank.admin.module.card.dto.UserFeeRateUpdateDTO;
import com.bank.admin.module.card.service.CardUserService;
import com.bank.admin.module.card.vo.CardUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理 Controller（含两级层级关系）
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/card/user")
@RequiredArgsConstructor
public class CardUserController {

    private final CardUserService cardUserService;

    @Operation(summary = "树形分页查询用户")
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<PageResult<CardUserVO>> page(@Valid CardUserQueryDTO query) {  // 使用同包下的 QueryDTO
        return Result.success(cardUserService.pageTree(query));
    }

    @Operation(summary = "获取所有启用的树形用户列表")
    @GetMapping("/tree")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<List<CardUserVO>> tree() {
        return Result.success(cardUserService.listTreeAll());
    }

    @Operation(summary = "获取启用的一级用户（下拉选择用）")
    @GetMapping("/list-active")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<List<CardUserVO>> listActive() {
        return Result.success(cardUserService.listActiveTopLevel());
    }

    @Operation(summary = "用户详情")
    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<CardUserVO> detail(@PathVariable Long id) {
        return Result.success(cardUserService.detail(id));
    }

    @Operation(summary = "新增用户")
    @Log(module = "用户管理", type = ActionTypeEnum.INSERT, description = "新增用户")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Long> save(@Valid @RequestBody CardUserSaveDTO dto) {
        return Result.success(cardUserService.save(dto));
    }

    @Operation(summary = "编辑用户")
    @Log(module = "用户管理", type = ActionTypeEnum.UPDATE, description = "编辑用户")
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Void> update(@Valid @RequestBody CardUserSaveDTO dto) {
        cardUserService.update(dto);
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @Log(module = "用户管理", type = ActionTypeEnum.DELETE, description = "删除用户[id=#id]")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        cardUserService.delete(id);
        return Result.success();
    }

    @Operation(summary = "修改手续费率（级联更新子用户）")
    @PutMapping("/update-fee-rate")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Log(module = "用户管理", type = ActionTypeEnum.UPDATE, description = "修改手续费率")
    public Result<Void> updateFeeRate(@Valid @RequestBody UserFeeRateUpdateDTO dto) {
        cardUserService.updateFeeRateCascade(dto);
        return Result.success();
    }
}
