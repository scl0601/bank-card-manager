package com.bank.admin.module.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体（不继承BaseEntity，日志不需要逻辑删除等字段）
 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人 */
    private String operator;

    /** 操作模块 */
    private String module;

    /** 操作类型：新增/编辑/删除/登录/标记等 */
    private String action;

    /** 操作描述 */
    private String description;

    /** 请求路径 */
    private String requestPath;

    /** 请求方法 */
    private String requestMethod;

    /** 请求参数（JSON，截断存储） */
    private String requestParams;

    /** 响应结果：0成功 1失败 */
    private Integer result;

    /** 失败原因 */
    private String errorMsg;

    /** 操作IP */
    private String clientIp;

    /** 执行耗时（毫秒） */
    private Long duration;

    /** 操作时间 */
    private LocalDateTime createTime;
}
