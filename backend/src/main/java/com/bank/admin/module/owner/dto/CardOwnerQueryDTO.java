package com.bank.admin.module.owner.dto;

import com.bank.admin.common.dto.PageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 持卡人分页查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "持卡人查询参数")
public class CardOwnerQueryDTO extends PageDTO {

    @Schema(description = "姓名（模糊）")
    private String name;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "状态：0正常 1禁用")
    private Integer status;
}
