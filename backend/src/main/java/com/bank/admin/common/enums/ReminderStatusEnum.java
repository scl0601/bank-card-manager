package com.bank.admin.common.enums;

import lombok.Getter;

/**
 * 提醒状态枚举
 */
@Getter
public enum ReminderStatusEnum {

    PENDING(0, "待处理"),
    HANDLED(1, "已处理"),
    IGNORED(2, "已忽略");

    private final int code;
    private final String desc;

    ReminderStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ReminderStatusEnum fromCode(int code) {
        for (ReminderStatusEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        throw new IllegalArgumentException("未知的提醒状态: " + code);
    }
}
