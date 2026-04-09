package com.bank.admin.module.auth.service;

import com.bank.admin.module.auth.dto.LoginDTO;
import com.bank.admin.module.auth.vo.LoginVO;

/**
 * 认证 Service 接口
 */
public interface AuthService {

    /**
     * 登录
     */
    LoginVO login(LoginDTO dto);
}
