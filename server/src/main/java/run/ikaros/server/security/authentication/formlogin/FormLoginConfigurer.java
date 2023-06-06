package run.ikaros.server.security.authentication.formlogin;

import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.stereotype.Component;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.server.security.authentication.SecurityConfigurer;

@Component
public class FormLoginConfigurer implements SecurityConfigurer {

    private final FormLoginSuccessHandler formLoginSuccessHandler;

    public FormLoginConfigurer(FormLoginSuccessHandler formLoginSuccessHandler) {
        this.formLoginSuccessHandler = formLoginSuccessHandler;
    }

    @Override
    public void configure(ServerHttpSecurity http) {
        http.formLogin()
            .authenticationSuccessHandler(formLoginSuccessHandler)
            .authenticationFailureHandler(new FormLoginFailureHandler())
        ;
    }
}
