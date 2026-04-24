package com.bank.admin.module.card.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户VO（含树形结构）
 */
@Data
@Schema(description = "用户VO")
public class CardUserVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "父用户ID（NULL=一级用户）")
    private Long parentId;

    @Schema(description = "父用户名称")
    private String parentName;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "手续费率%")
    private BigDecimal feeRate;

    @Schema(description = "生效手续费率%（继承或自身）")
    private BigDecimal effectiveFeeRate;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "状态：0正常 1停用")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "关联银行卡数")
    private Long cardCount;

    @Schema(description = "子用户列表")
    private List<CardUserVO> children;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
