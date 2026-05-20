package com.bank.admin.module.announcement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 公告保存参数
 */
@Data
@Schema(description = "公告保存参数")
public class AnnouncementSaveDTO {

    @NotBlank(message = "公告内容不能为空")
    @Schema(description = "公告内容，按换行展示为更新点", required = true)
    private String content;

    @Schema(description = "是否置顶：0否 1是")
    private Integer pinned;

    @Schema(description = "排序号，越大越靠前")
    private Integer sortOrder;
}
