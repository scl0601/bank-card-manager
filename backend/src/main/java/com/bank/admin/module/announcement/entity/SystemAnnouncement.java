package com.bank.admin.module.announcement.entity;

import com.bank.admin.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统公告主表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_announcement")
public class SystemAnnouncement extends BaseEntity {

    /** 公告内容，纯文本按换行展示 */
    private String content;

    /** 状态：0草稿 1已发布 2已下线 */
    private Integer status;

    /** 是否置顶：0否 1是 */
    private Integer pinned;

    /** 排序号，越大越靠前 */
    private Integer sortOrder;

    /** 发布时间 */
    private LocalDateTime publishTime;
}
