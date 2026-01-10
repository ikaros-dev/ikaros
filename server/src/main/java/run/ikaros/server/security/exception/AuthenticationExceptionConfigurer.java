package run.ikaros.server.security.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.stereotype.Component;
import run.ikaros.server.security.authentication.SecurityConfigurer;

@Slf4j
@Component
public class AuthenticationExceptionConfigurer implements SecurityConfigurer {
    @Override
    public void configure(ServerHttpSecurity http) {
        http.exceptionHandling((exceptionHandlingSpec ->
            exceptionHandlingSpec.authenticationEntryPoint(
                new JsonServerAuthenticationEntryPoint()
            )));
    }
}
