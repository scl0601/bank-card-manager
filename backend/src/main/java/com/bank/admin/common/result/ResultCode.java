package com.bank.admin.common.result;

import lombok.Getter;

/**
 * 统一响应码枚举
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    DATA_NOT_FOUND(404, "数据不存在"),
    OPERATION_FAILED(500, "操作失败"),
    USERNAME_OR_PASSWORD_ERROR(1001, "用户名或密码错误"),
    ACCOUNT_DISABLED(1002, "账号已被禁用"),
    TOKEN_INVALID(1003, "Token无效"),
    DATA_ALREADY_EXISTS(1004, "数据已存在");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
