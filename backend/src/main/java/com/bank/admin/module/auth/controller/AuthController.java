package com.bank.admin.module.auth.controller;

import com.bank.admin.common.annotation.Log;
import com.bank.admin.common.enums.ActionTypeEnum;
import com.bank.admin.common.result.Result;
import com.bank.admin.module.auth.dto.LoginDTO;
import com.bank.admin.module.auth.service.AuthService;
import com.bank.admin.module.auth.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证 Controller
 */
@Tag(name = "登录认证", description = "登录、登出")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录")
    @Log(module = "系统认证", type = ActionTypeEnum.LOGIN, description = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @Operation(summary = "登出（前端清除Token即可）")
    @Log(module = "系统认证", type = ActionTypeEnum.LOGOUT, description = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
