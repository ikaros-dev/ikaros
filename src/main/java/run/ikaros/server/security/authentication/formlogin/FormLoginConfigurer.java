package run.ikaros.server.security.authentication.formlogin;

import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.stereotype.Component;
import run.ikaros.server.security.authentication.SecurityConfigurer;

@Component
public class FormLoginConfigurer implements SecurityConfigurer {

    @Override
    public void configure(ServerHttpSecurity http) {
        http.formLogin()
            .authenticationSuccessHandler(new FormLoginSuccessHandler())
            .authenticationFailureHandler(new FormLoginFailureHandler())
        ;
    }
}
