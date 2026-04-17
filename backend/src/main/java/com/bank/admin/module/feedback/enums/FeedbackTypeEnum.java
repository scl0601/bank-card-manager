package com.bank.admin.module.feedback.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 反馈类型枚举
 */
@Getter
@AllArgsConstructor
public enum FeedbackTypeEnum {

    BUG(0, "功能异常"),
    UI(1, "界面体验"),
    DATA(2, "数据问题"),
    PERMISSION(3, "权限问题"),
    OTHER(4, "其他");

    private final Integer code;
    private final String desc;

    public static String getDesc(Integer code) {
        if (code == null) return "";
        for (FeedbackTypeEnum e : values()) {
            if (e.code.equals(code)) return e.desc;
        }
        return "";
    }
}
