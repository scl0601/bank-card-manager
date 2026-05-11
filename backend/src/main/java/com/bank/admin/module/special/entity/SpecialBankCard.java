package com.bank.admin.module.special.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bank.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 特殊通道银行卡。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("special_bank_card")
public class SpecialBankCard extends BaseEntity {

    /** 关联 card_user.id */
    private Long userId;

    /** 银行名称 */
    private String bankName;

    /** 卡号后四位 / 卡片标识 */
    private String cardNoLast4;

    /** 总金额：卡片总额度 */
    private BigDecimal totalAmount;

    /** 有效期截止（原样保存用户输入，如 06/28） */
    private String expireDate;

    /** 0正常 1停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
