package com.bank.admin.module.auth.entity;

import com.bank.admin.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bank_sys_user")
public class SysUser extends BaseEntity {

    /** 用户名 */
    private String username;

    /** 密码（BCrypt加密） */
    private String password;

    /** 昵称 */
    private String nickname;

    /**
     * 角色：ADMIN / OPERATOR / VIEWER
     */
    private String role;

    /**
     * 状态：0正常 1禁用
     */
    private Integer status;
}
