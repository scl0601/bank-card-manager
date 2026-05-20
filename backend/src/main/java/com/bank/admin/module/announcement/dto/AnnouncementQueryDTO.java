package com.bank.admin.module.announcement.dto;

import com.bank.admin.common.dto.PageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公告分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "公告分页查询参数")
public class AnnouncementQueryDTO extends PageDTO {

    @Schema(description = "公告内容关键词")
    private String content;

    @Schema(description = "状态：0草稿 1已发布 2已下线")
    private Integer status;
}
