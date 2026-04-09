package com.bank.admin.module.bill.entity;

import com.bank.admin.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 账单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("card_bill")
public class CardBill extends BaseEntity {

    /** 银行卡ID */
    private Long cardId;

    /** 持卡人ID */
    private Long ownerId;

    /** 账单月份（格式：yyyy-MM） */
    private String billMonth;

    /** 账单金额 */
    private BigDecimal billAmount;

    /** 最低还款额 */
    private BigDecimal minPayAmount;

    /** 还款日 */
    private LocalDate repayDate;

    /** 实际还款金额 */
    private BigDecimal actualPayAmount;

    /** 实际还款日期 */
    private LocalDate actualPayDate;

    /**
     * 账单状态：0待还款 1已还清 2部分还款 3逾期
     */
    private Integer status;

    /** 备注 */
    private String remark;
}
