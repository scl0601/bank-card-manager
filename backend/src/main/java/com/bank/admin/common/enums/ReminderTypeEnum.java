package com.bank.admin.common.enums;

import lombok.Getter;

/**
 * 提醒类型枚举
 */
@Getter
public enum ReminderTypeEnum {

    BILL_UPCOMING(1, "即将到期"),
    BILL_DUE_TODAY(2, "今日到期"),
    BILL_OVERDUE(3, "已逾期"),
    CARD_EXPIRING(4, "卡片即将过期");

    private final int code;
    private final String desc;

    ReminderTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ReminderTypeEnum fromCode(int code) {
        for (ReminderTypeEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        throw new IllegalArgumentException("未知的提醒类型: " + code);
    }
}
