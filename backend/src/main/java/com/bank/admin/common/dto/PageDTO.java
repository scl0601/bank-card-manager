package com.bank.admin.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页查询基类
 */
@Data
@Schema(description = "分页查询基类")
public class PageDTO {

    @Schema(description = "当前页码，默认1")
    @Min(value = 1, message = "页码最小为1")
    private long current = 1;

    @Schema(description = "每页条数，默认10")
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private long size = 10;
}
