package com.bank.admin.module.special.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bank.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 特殊通道月账单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("special_card_bill")
public class SpecialCardBill extends BaseEntity {

    /** 特殊银行卡ID */
    private Long cardId;

    /** 冗余用户ID，关联 card_user.id */
    private Long userId;

    /** 年月，格式 yyyy-MM */
    private String billMonth;

    /** 年份 */
    private Integer billYear;

    /** 月份 */
    private Integer billMonthNo;

    /** 历史兼容字段，当前页面使用银行卡卡片额度展示 */
    private BigDecimal monthlyTotalBillAmount;

    /** 账单日 */
    private Integer billDay;

    /** 还款日 */
    private Integer repaymentDay;

    /** 本月账单金额，手动填写 */
    private BigDecimal billAmount;

    /** 本月账单金额是否核实 */
    private Boolean billAmountVerified;

    /** 小焕还款，手动填写 */
    private BigDecimal xiaohuanRepayAmount;

    /** 小焕还款是否核实 */
    private Boolean xiaohuanRepayVerified;

    /** 客户还款，手动填写 */
    private BigDecimal customerRepayAmount;

    /** 客户还款是否核实 */
    private Boolean customerRepayVerified;

    /** 小焕消费，手动填写 */
    private BigDecimal xiaohuanConsumeAmount;

    /** 小焕消费是否核实 */
    private Boolean xiaohuanConsumeVerified;

    /** 客户需要，手动填写 */
    private BigDecimal customerNeedAmount;

    /** 客户需要是否核实 */
    private Boolean customerNeedVerified;

    /** 客户消费，手动填写 */
    private BigDecimal customerConsumeAmount;

    /** 客户消费是否核实 */
    private Boolean customerConsumeVerified;

    /** 差额 = 小焕还款 - 小焕消费 */
    private BigDecimal diffAmount;

    /** 余额，手动填写，仅账单页展示 */
    private BigDecimal balance;

    /** 费率快照，百分比，如1表示1% */
    private BigDecimal feeRate;

    /** 还款手续费 = 本月账单金额 × 费率 */
    private BigDecimal repaymentFee;

    /** 消费手续费 = (小焕消费 + 客户消费) × 费率 */
    private BigDecimal consumeFee;

    /** 利息，手动填写 */
    private BigDecimal interestAmount;

    /** 滞纳金，手动填写 */
    private BigDecimal lateFeeAmount;

    /** 分期费，手动填写 */
    private BigDecimal installmentFeeAmount;

    /** 收益统计总计 */
    private BigDecimal profitTotalAmount;

    /** 备注 */
    private String remark;
}
