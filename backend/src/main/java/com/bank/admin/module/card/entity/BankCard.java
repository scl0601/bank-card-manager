package com.bank.admin.module.card.entity;

import com.bank.admin.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 银行卡实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bank_card")
public class BankCard extends BaseEntity {

    /** 持卡人ID */
    private Long ownerId;

    /** 银行名称 */
    private String bankName;

    /** 卡号（加密存储） */
    private String cardNo;

    /** 卡号后四位（明文，用于展示） */
    private String cardNoLast4;

    /** 卡片类型：1借记卡 2信用卡 */
    private Integer cardType;

    /** 信用额度（信用卡用） */
    private BigDecimal creditLimit;

    /** 当前余额 */
    private BigDecimal balance;

    /** 已用额度（信用卡用） */
    private BigDecimal usedAmount;

    /** 账单日（每月几号，信用卡用） */
    private Integer billDay;

    /** 还款日（每月几号，信用卡用） */
    private Integer repayDay;

    /** 有效期截止（格式：YYYY-MM） */
    private String expireDate;

    /**
     * 状态：0正常 1冻结 2注销
     */
    private Integer status;

    /** 备注 */
    private String remark;
}
