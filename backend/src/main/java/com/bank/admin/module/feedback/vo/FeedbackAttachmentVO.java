package com.bank.admin.module.feedback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 附件 VO
 */
@Data
@Schema(description = "反馈附件信息")
public class FeedbackAttachmentVO {

    @Schema(description = "附件ID")
    private Long id;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "文件访问地址")
    private String fileUrl;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文件后缀")
    private String fileSuffix;

    @Schema(description = "MIME类型")
    private String contentType;

    @Schema(description = "排序")
    private Integer sortNo;
}
