package run.ikaros.server.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public class SecurityContextHolderUtils {
    public static SecurityUser getCurrentSecurityUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 未认证或匿名用户
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUser user) {
            return user;
        }

        return null;
    }

    public static UUID getCurrentUserId() {
        SecurityUser securityUser = getCurrentSecurityUser();
        return securityUser != null ? securityUser.getId() : null;
    }
}
