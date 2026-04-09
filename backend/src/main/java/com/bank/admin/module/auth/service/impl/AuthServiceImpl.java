package com.bank.admin.module.auth.service.impl;

import com.bank.admin.common.exception.BusinessException;
import com.bank.admin.common.result.ResultCode;
import com.bank.admin.common.util.JwtUtil;
import com.bank.admin.module.auth.dto.LoginDTO;
import com.bank.admin.module.auth.entity.SysUser;
import com.bank.admin.module.auth.mapper.SysUserMapper;
import com.bank.admin.module.auth.service.AuthService;
import com.bank.admin.module.auth.vo.LoginVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证 ServiceImpl
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginVO login(LoginDTO dto) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, dto.getUsername())
                        .eq(SysUser::getIsDeleted, 0));

        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        String token = jwtUtil.generateToken(user.getUsername());
        log.info("[登录] 用户 {} 登录成功", user.getUsername());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        return vo;
    }
}
