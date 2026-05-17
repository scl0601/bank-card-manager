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

import java.util.List;

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
        List<SysUser> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, dto.getUsername())
                        .eq(SysUser::getIsDeleted, 0)
                        .orderByDesc(SysUser::getUpdateTime)
                        .orderByDesc(SysUser::getId));

        SysUser user = users.stream()
                .filter(item -> passwordMatches(dto.getPassword(), item))
                .findFirst()
                .orElse(null);

        if (user == null) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        String token;
        try {
            token = jwtUtil.generateToken(user.getUsername());
        } catch (Exception e) {
            log.error("[登录] 生成 JWT 失败，请检查 jwt.secret 长度/配置", e);
            throw new BusinessException(ResultCode.OPERATION_FAILED, "登录配置异常，请联系管理员");
        }
        log.info("[登录] 用户 {} 登录成功", user.getUsername());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        vo.setDataScope(user.getDataScope());
        return vo;
    }

    private boolean passwordMatches(String rawPassword, SysUser user) {
        if (user == null || user.getPassword() == null || user.getPassword().isBlank()) {
            return false;
        }
        try {
            return passwordEncoder.matches(rawPassword, user.getPassword());
        } catch (Exception e) {
            log.warn("[登录] 用户 {} 的密码哈希格式异常，已跳过", user.getUsername(), e);
            return false;
        }
    }
}
