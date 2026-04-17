package com.bank.admin.module.feedback.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 反馈状态枚举
 */
@Getter
@AllArgsConstructor
public enum FeedbackStatusEnum {

    PENDING(0, "待处理"),
    IN_PROGRESS(1, "修复中"),
    RESOLVED(2, "已解决"),
    CLOSED(3, "已关闭");

    private final Integer code;
    private final String desc;

    public static String getDesc(Integer code) {
        if (code == null) return "";
        for (FeedbackStatusEnum e : values()) {
            if (e.code.equals(code)) return e.desc;
        }
        return "";
    }
}
