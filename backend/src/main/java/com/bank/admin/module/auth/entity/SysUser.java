package com.bank.admin.module.auth.entity;

import com.bank.admin.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bank_sys_user")
public class SysUser extends BaseEntity {

    private String username;

    private String password;

    private String nickname;

    /**
     * ADMIN / OPERATOR / VIEWER.
     */
    private String role;

    /**
     * ALL = all data, SELF = only records created by this account.
     */
    @TableField("data_scope")
    private String dataScope;

    /**
     * 0 = enabled, 1 = disabled.
     */
    private Integer status;
}
