package com.bank.admin.module.transaction.entity;

import com.bank.admin.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 流水记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("card_transaction")
public class CardTransaction extends BaseEntity {

    /** 银行卡ID */
    private Long cardId;

    /** 持卡人ID */
    private Long ownerId;

    /** 来源方ID（冗余，用于聚合查询） */
    private Long supplierId;

    /**
     * 交易类型：1收入 2支出
     */
    private Integer txType;

    /** 交易金额（正数） */
    private BigDecimal amount;

    /** 交易日期 */
    private LocalDate txDate;

    /** 交易描述 */
    private String description;

    /** 交易对手方（商户/转账人等） */
    private String counterpart;

    /** 余额快照（交易后余额） */
    private BigDecimal balanceSnapshot;

    /** 备注 */
    private String remark;
}
