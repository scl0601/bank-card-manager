package com.bank.admin.module.card.dto;

import com.bank.admin.common.dto.PageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 银行卡分页查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "银行卡查询参数")
public class BankCardQueryDTO extends PageDTO {

    @Schema(description = "持卡人ID")
    private Long ownerId;

    @Schema(description = "持卡人姓名（模糊）")
    private String ownerName;

    @Schema(description = "银行名称（模糊）")
    private String bankName;

    @Schema(description = "卡号后四位")
    private String cardNoLast4;

    @Schema(description = "卡片类型：1借记卡 2信用卡")
    private Integer cardType;

    @Schema(description = "状态：0正常 1冻结 2注销")
    private Integer status;
}
