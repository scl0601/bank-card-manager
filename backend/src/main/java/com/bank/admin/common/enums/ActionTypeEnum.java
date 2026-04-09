package com.bank.admin.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作类型枚举
 */
@Getter
@AllArgsConstructor
public enum ActionTypeEnum {

    INSERT(0, "新增"),
    UPDATE(1, "编辑"),
    DELETE(2, "删除"),
    LOGIN(3, "登录"),
    LOGOUT(4, "登出"),
    EXPORT(5, "导出"),
    IMPORT(6, "导入"),
    OTHER(9, "其他");

    private final Integer code;
    private final String desc;
}
