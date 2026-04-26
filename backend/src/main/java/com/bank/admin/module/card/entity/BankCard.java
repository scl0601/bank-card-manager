package com.bank.admin.module.card.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bank.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 银行卡实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bank_card")
public class BankCard extends BaseEntity {

    /** 用户ID（关联 card_user） */
    private Long userId;

    /** 银行名称 */
    private String bankName;

    /** 卡号后四位（明文） */
    private String cardNoLast4;

    /** 卡片归属人关系：本人/配偶/子女等 */
    private String ownerRelation;

    /** 卡片归属人姓名（可选） */
    private String ownerName;

    /** 卡片类型：1借记卡 2信用卡 */
    private Integer cardType;

    /** 信用额度（信用卡用） */
    private BigDecimal creditLimit;

    /** 当前余额（借记卡用） */
    private BigDecimal balance;

    /** 总额度（借记卡用） */
    private BigDecimal totalLimit;

    /** 账单日（每月几号，信用卡用） */
    private Integer billDay;

    /** 还款日（每月几号，信用卡用） */
    private Integer repayDay;

    /** 有效期截止（原样保存用户输入，如 06/28、06-28、06月28年） */
    private String expireDate;

    /**
     * 状态：0正常 1冻结 2注销
     */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 还款方式：cloudpay云闪付 invoice开票 */
    private String repayMethod;

    /** 是否核实（云闪付时有效） */
    private Boolean verified;

    // ========== 非数据库字段（VO展示用） ==========
    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private BigDecimal effectiveFeeRate;
}
