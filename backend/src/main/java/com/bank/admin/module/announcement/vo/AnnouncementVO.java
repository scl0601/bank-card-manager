package com.bank.admin.module.announcement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 公告展示信息
 */
@Data
@Schema(description = "公告展示信息")
public class AnnouncementVO {

    private Long id;
    private String content;
    private Integer status;
    private String statusDesc;
    private Integer pinned;
    private Integer sortOrder;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
    private String updateBy;
    private LocalDateTime readTime;
    private LocalDate popupDate;
    private LocalDate silentDate;
    private Boolean read;
    private Boolean shouldPopup;
}
