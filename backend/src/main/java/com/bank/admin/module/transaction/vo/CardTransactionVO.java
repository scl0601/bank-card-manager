package com.bank.admin.module.transaction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 流水VO
 */
@Data
@Schema(description = "流水VO")
public class CardTransactionVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "银行卡ID")
    private Long cardId;

    @Schema(description = "卡号后四位")
    private String cardNoLast4;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "持卡人ID")
    private Long ownerId;

    @Schema(description = "持卡人姓名")
    private String ownerName;

    @Schema(description = "交易类型：1收入 2支出")
    private Integer txType;

    @Schema(description = "交易类型描述")
    private String txTypeDesc;

    @Schema(description = "交易金额")
    private BigDecimal amount;

    @Schema(description = "交易日期")
    private LocalDate txDate;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "交易对手方")
    private String counterpart;

    @Schema(description = "余额快照")
    private BigDecimal balanceSnapshot;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
