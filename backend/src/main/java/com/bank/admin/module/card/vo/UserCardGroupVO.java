package com.bank.admin.module.card.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 按用户分组的银行卡视图
 */
@Data
@Schema(description = "按用户分组的银行卡视图")
public class UserCardGroupVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "手续费率%")
    private BigDecimal feeRate;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "卡片数量")
    private int cardCount;

    @Schema(description = "银行卡列表")
    private List<BankCardVO> cards;
}
