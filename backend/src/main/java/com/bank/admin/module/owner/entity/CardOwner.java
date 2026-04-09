package com.bank.admin.module.owner.entity;

import com.bank.admin.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 持卡人实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("card_owner")
public class CardOwner extends BaseEntity {

    /** 姓名 */
    private String name;

    /** 手机号（脱敏存储） */
    private String phone;

    /** 身份证号（加密存储） */
    private String idCard;

    /** 备注 */
    private String remark;

    /**
     * 状态：0正常 1禁用
     */
    private Integer status;
}
