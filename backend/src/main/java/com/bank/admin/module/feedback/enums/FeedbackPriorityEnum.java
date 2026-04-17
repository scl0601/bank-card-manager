package com.bank.admin.module.feedback.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 反馈优先级枚举
 */
@Getter
@AllArgsConstructor
public enum FeedbackPriorityEnum {

    LOW(0, "低"),
    MEDIUM(1, "中"),
    HIGH(2, "高"),
    URGENT(3, "紧急");

    private final Integer code;
    private final String desc;

    public static String getDesc(Integer code) {
        if (code == null) return "";
        for (FeedbackPriorityEnum e : values()) {
            if (e.code.equals(code)) return e.desc;
        }
        return "";
    }
}
