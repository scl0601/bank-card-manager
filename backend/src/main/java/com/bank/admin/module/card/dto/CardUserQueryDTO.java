package com.bank.admin.module.card.dto;

import com.bank.admin.common.dto.PageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户查询参数")
public class CardUserQueryDTO extends PageDTO {

    @Schema(description = "姓名（模糊）")
    private String name;

    @Schema(description = "电话（模糊）")
    private String phone;

    @Schema(description = "状态：0正常 1停用")
    private Integer status;

    @Schema(description = "是否只查一级用户")
    private Boolean topLevelOnly;
}
