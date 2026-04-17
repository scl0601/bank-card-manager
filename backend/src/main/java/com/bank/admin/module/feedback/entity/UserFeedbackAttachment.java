package com.bank.admin.module.feedback.entity;

import com.bank.admin.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 反馈附件表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_feedback_attachment")
@Schema(description = "反馈附件")
public class UserFeedbackAttachment extends BaseEntity {

    @Schema(description = "关联反馈ID")
    private Long feedbackId;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "文件访问地址")
    private String fileUrl;

    @Schema(description = "服务器存储路径")
    private String filePath;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文件后缀（小写，不含点）")
    private String fileSuffix;

    @Schema(description = "MIME类型")
    private String contentType;

    @Schema(description = "排序")
    private Integer sortNo;
}
