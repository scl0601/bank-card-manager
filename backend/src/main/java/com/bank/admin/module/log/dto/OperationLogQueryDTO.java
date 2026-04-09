package com.bank.admin.module.log.dto;

import com.bank.admin.common.dto.PageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 操作日志查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "操作日志查询参数")
public class OperationLogQueryDTO extends PageDTO {

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "操作模块")
    private String module;

    @Schema(description = "操作结果：0成功 1失败")
    private Integer result;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;
}
