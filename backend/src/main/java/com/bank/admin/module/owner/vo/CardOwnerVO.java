package com.bank.admin.module.owner.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 持卡人VO
 */
@Data
@Schema(description = "持卡人VO")
public class CardOwnerVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "手机号（脱敏）")
    private String phone;

    @Schema(description = "身份证号（脱敏）")
    private String idCard;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态：0正常 1禁用")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "名下银行卡数量")
    private Integer cardCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
