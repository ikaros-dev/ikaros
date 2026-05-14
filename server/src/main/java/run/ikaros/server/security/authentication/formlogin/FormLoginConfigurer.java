package run.ikaros.server.security.authentication.formlogin;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;
import run.ikaros.server.security.authentication.SecurityConfigurer;

@Component
public class FormLoginConfigurer implements SecurityConfigurer {

    private final FormLoginSuccessHandler formLoginSuccessHandler;
    private final FormLoginFailureHandler formLoginFailureHandler;

    public FormLoginConfigurer(FormLoginSuccessHandler formLoginSuccessHandler,
                               FormLoginFailureHandler formLoginFailureHandler) {
        this.formLoginSuccessHandler = formLoginSuccessHandler;
        this.formLoginFailureHandler = formLoginFailureHandler;
    }

    @Override
    public void configure(HttpSecurity http) {
        http.formLogin((formLogin) ->
            formLogin
                // .successHandler(formLoginSuccessHandler)
                .failureHandler(formLoginFailureHandler)
        );
    }
}
