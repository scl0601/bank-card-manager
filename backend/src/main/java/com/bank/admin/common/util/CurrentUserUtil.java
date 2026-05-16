package com.bank.admin.common.util;

import com.bank.admin.module.auth.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Current authenticated user helpers.
 */
public final class CurrentUserUtil {

    public static final String DATA_SCOPE_ALL = "ALL";
    public static final String DATA_SCOPE_SELF = "SELF";

    private CurrentUserUtil() {
    }

    public static String getUsernameOrDefault(String defaultValue) {
        LoginUser loginUser = getLoginUser();
        if (loginUser != null && loginUser.getUsername() != null) {
            return loginUser.getUsername();
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null
                && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return defaultValue;
    }

    public static LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    public static boolean isSelfDataScope() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null && DATA_SCOPE_SELF.equalsIgnoreCase(loginUser.getDataScope());
    }
}
