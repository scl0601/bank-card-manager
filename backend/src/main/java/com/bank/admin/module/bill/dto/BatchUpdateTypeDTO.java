package com.bank.admin.module.bill.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Data
@Schema(description = "批量修改明细收支状态DTO")
public class BatchUpdateTypeDTO {

    @NotEmpty(message = "明细ID列表不能为空")
    @Schema(description = "明细ID列表")
    private List<Long> ids;

    @NotNull(message = "明细收支状态不能为空")
    @Schema(description = "明细收支状态：0=支出 1=收入")
    private Integer detailType;
}
