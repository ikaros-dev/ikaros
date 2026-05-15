package run.ikaros.server.security.authentication.formlogin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import run.ikaros.api.store.entity.User;
import run.ikaros.server.security.SecurityUser;
import run.ikaros.server.store.mapper.UserMapper;

class FormLoginSuccessHandlerTest {

    private UserMapper userMapper;
    private FormLoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        handler = new FormLoginSuccessHandler(userMapper);
        SecurityContextHolder.clearContext();
    }

    @Test
    void onAuthenticationSuccess_shouldWriteUserJson() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("secret");
        user.setNickname("Test User");
        user.setEnable(true);
        user.setNonLocked(true);

        SecurityUser securityUser = new SecurityUser(user, List.of());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(securityUser);
        when(authentication.getName()).thenReturn("testuser");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).setContentType("application/json");
        String body = stringWriter.toString();
        assert body.contains("testuser");
        // Password should be erased after success
        assert securityUser.getPassword() == null;
    }
}
