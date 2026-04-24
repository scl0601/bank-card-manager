package com.bank.admin.module.card.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bank.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 用户实体（合并持卡人+来源方，支持两级层级）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("card_user")
public class CardUser extends BaseEntity {

    /** 父用户ID（NULL=一级用户） */
    private Long parentId;

    /** 姓名/名称 */
    private String name;

    /** 联系电话 */
    private String phone;

    /** 手续费率%（仅一级有效） */
    private BigDecimal feeRate;

    /** 备注 */
    private String remark;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态：0正常 1停用 */
    private Integer status;

    /** 非数据库字段：关联银行卡数量 */
    @TableField(exist = false)
    private Long cardCount;

    /** 非数据库字段：生效的手续费率（继承或自身） */
    @TableField(exist = false)
    private BigDecimal effectiveFeeRate;

    /** 非数据库字段：子用户列表 */
    @TableField(exist = false)
    private java.util.List<CardUser> children;
}
