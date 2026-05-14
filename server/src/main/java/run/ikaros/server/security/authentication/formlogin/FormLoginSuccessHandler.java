package run.ikaros.server.security.authentication.formlogin;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import run.ikaros.api.store.entity.User;
import run.ikaros.server.infra.utils.JsonUtils;
import run.ikaros.server.security.SecurityUser;
import run.ikaros.server.store.mapper.UserMapper;


/**
 * Return a user detail json when login success.
 */
@Slf4j
@Component
public class FormLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final UserMapper userMapper;

    public FormLoginSuccessHandler(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication)
        throws IOException, ServletException {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        User user = securityUser.getUser();
        log.debug("Set user info to security context with username={}.",
            securityUser.getUsername());
        response.setContentType(MediaType.APPLICATION_JSON.toString());
        securityUser.eraseCredentials();
        response.getWriter().write(JsonUtils.obj2Json(user));
    }
}
