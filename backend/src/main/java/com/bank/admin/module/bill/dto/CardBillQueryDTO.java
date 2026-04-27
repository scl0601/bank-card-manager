package com.bank.admin.module.bill.dto;

import com.bank.admin.common.dto.PageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 账单分页查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "账单查询参数")
public class CardBillQueryDTO extends PageDTO {

    @Schema(description = "银行卡ID")
    private Long cardId;

    @Schema(description = "银行卡ID列表，逗号分隔")
    private String cardIds;

    @Schema(description = "持卡人ID")
    private Long ownerId;

    @Schema(description = "持卡人姓名关键字")
    private String ownerName;

    @Schema(description = "银行卡关键字（银行名称/卡号后四位）")
    private String cardName;

    @Schema(description = "账单月份（yyyy-MM）")
    private String billMonth;

    @Schema(description = "统计年份")
    private Integer year;

    @Schema(description = "账单开始月份（yyyy-MM）")
    private String startBillMonth;

    @Schema(description = "账单结束月份（yyyy-MM）")
    private String endBillMonth;

    @Schema(description = "状态：0待还款 1已还清 2部分还款 3逾期")
    private Integer status;

    @Schema(description = "手续费是否已支付")
    private Boolean feePaid;
}
