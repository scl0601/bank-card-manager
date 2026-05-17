package com.bank.admin.module.auth.service.impl;

import com.bank.admin.module.auth.entity.SysUser;
import com.bank.admin.module.auth.mapper.SysUserMapper;
import com.bank.admin.module.auth.security.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getUsername, username)
                                .eq(SysUser::getIsDeleted, 0)
                                .orderByDesc(SysUser::getUpdateTime)
                                .orderByDesc(SysUser::getId))
                .stream()
                .findFirst()
                .orElse(null);

        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        return new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getNickname(),
                user.getRole(),
                user.getDataScope(),
                user.getStatus(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}
