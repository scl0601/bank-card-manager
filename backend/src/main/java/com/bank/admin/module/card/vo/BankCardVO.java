package com.bank.admin.module.card.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 银行卡VO
 */
@Data
@Schema(description = "银行卡VO")
public class BankCardVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户姓名")
    private String userName;

    @Schema(description = "生效手续费率%")
    private BigDecimal effectiveFeeRate;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "卡号后四位")
    private String cardNoLast4;

    @Schema(description = "卡片类型：1借记卡 2信用卡")
    private Integer cardType;

    @Schema(description = "卡片类型描述")
    private String cardTypeDesc;

    @Schema(description = "信用额度")
    private BigDecimal creditLimit;

    @Schema(description = "账单日")
    private Integer billDay;

    @Schema(description = "还款日")
    private Integer repayDay;

    @Schema(description = "有效期截止（原样展示用户输入）")
    private String expireDate;

    @Schema(description = "状态：0正常 1冻结 2注销")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "APP：cloudpay云闪付 wechat微信 alipay支付宝 other其他")
    private String repayMethod;

    @Schema(description = "是否已核实")
    private Boolean verified;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
