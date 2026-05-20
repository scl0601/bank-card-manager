package com.bank.admin.module.announcement.entity;

import com.bank.admin.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 公告用户阅读/弹窗状态
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_announcement_user_state")
public class SystemAnnouncementUserState extends BaseEntity {

    @TableField("announcement_id")
    private Long announcementId;

    private String username;

    private LocalDateTime readTime;

    private LocalDate popupDate;

    private LocalDate silentDate;
}
