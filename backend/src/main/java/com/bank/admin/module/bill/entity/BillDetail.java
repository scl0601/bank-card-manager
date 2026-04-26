package com.bank.admin.module.bill.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bank.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 账单明细实体（支出/收入流水记录）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bill_detail")
public class BillDetail extends BaseEntity {

    /** 关联账单ID */
    private Long billId;

    /** 明细日期 */
    private LocalDate detailDate;

    /** 描述 */
    private String description;

    /** 金额（正=收入 负=支出） */
    private BigDecimal amount;

    /**
     * 明细类型：0=支出 1=收入
     */
    private Integer detailType;

    /** 备注 */
    private String remark;
}
