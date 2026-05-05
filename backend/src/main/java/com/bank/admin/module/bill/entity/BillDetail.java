package com.bank.admin.module.bill.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bank.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 账单明细实体（消费/还款流水记录）
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

    /** 金额（正=还款 负=消费） */
    private BigDecimal amount;

    /**
     * 明细类型：0=消费 1=还款
     */
    private Integer detailType;

    /** 备注 */
    private String remark;
}
