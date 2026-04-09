package com.bank.admin.module.bill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 账单VO
 */
@Data
@Schema(description = "账单VO")
public class CardBillVO {

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

    @Schema(description = "账单月份")
    private String billMonth;

    @Schema(description = "账单金额")
    private BigDecimal billAmount;

    @Schema(description = "最低还款额")
    private BigDecimal minPayAmount;

    @Schema(description = "还款日")
    private LocalDate repayDate;

    @Schema(description = "实际还款金额")
    private BigDecimal actualPayAmount;

    @Schema(description = "实际还款日期")
    private LocalDate actualPayDate;

    @Schema(description = "状态：0待还款 1已还清 2部分还款 3逾期")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
