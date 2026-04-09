package com.bank.admin.common.annotation;

import com.bank.admin.common.enums.ActionTypeEnum;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 标记在 Controller 方法上，AOP 切面会自动记录操作日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /** 操作模块名称，如 "持卡人管理"、"银行卡管理" */
    String module();

    /** 操作类型 */
    ActionTypeEnum type();

    /** 操作描述（支持从参数中提取动态内容） */
    String description() default "";
}
