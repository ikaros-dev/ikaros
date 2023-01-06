package run.ikaros.server.security.authentication.formlogin;

import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import run.ikaros.server.security.authentication.SecurityConfigurer;

@Component
public class FormLoginConfigurer implements SecurityConfigurer {

    private final ServerResponse.Context context;

    public FormLoginConfigurer(ServerResponse.Context context) {
        this.context = context;
    }

    @Override
    public void configure(ServerHttpSecurity http) {
        http.formLogin()
            .authenticationSuccessHandler(new FormLoginSuccessHandler(context))
            .authenticationFailureHandler(new FormLoginFailureHandler(context))
        ;
    }
}
