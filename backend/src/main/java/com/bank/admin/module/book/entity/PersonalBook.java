package com.bank.admin.module.book.entity;

import com.bank.admin.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 个人记账实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("personal_book")
public class PersonalBook extends BaseEntity {

    /** 记账日期 */
    private LocalDate bookDate;

    /**
     * 记账类型：1收入 2支出
     */
    private Integer bookType;

    /** 金额（正数） */
    private BigDecimal amount;

    /** 分类ID */
    private Long categoryId;

    /** 描述/备注 */
    private String description;

    /** 关联银行卡ID（可选） */
    private Long cardId;

    /** 支付/收款账户ID */
    private Long accountId;

    /** 转入账户ID（转账时使用） */
    private Long targetAccountId;

    /** 记账时间 */
    private LocalTime bookTime;

    /** 商家/对象 */
    private String merchant;
}
