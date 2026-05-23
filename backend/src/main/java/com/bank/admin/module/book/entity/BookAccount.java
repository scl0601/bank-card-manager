package com.bank.admin.module.book.entity;

import com.bank.admin.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Personal bookkeeping account.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("book_account")
public class BookAccount extends BaseEntity {

    /** Account name, for example cash, WeChat, Alipay. */
    private String name;

    /** 1 cash, 2 bank card, 3 e-wallet, 4 credit, 5 other. */
    private Integer accountType;

    /** Opening balance entered by the user. */
    private BigDecimal initialBalance;

    /** Current balance maintained by bookkeeping entries. */
    private BigDecimal currentBalance;

    /** 0 active, 1 disabled. */
    private Integer status;

    /** Sort order. */
    private Integer sortOrder;

    /** Remark. */
    private String remark;
}
