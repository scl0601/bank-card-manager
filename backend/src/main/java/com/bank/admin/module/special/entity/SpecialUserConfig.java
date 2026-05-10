package com.bank.admin.module.special.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bank.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 特殊通道用户配置。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("special_user_config")
public class SpecialUserConfig extends BaseEntity {

    /** 关联 card_user.id */
    private Long userId;

    /** 0启用 1停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
