package run.ikaros.server.security.authentication.logout;


import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;
import run.ikaros.server.security.authentication.SecurityConfigurer;

@Component
public class LogoutConfigurer implements SecurityConfigurer {

    @Override
    public void configure(HttpSecurity http) {
        http.logout(logoutSpec ->
            logoutSpec.logoutSuccessHandler(new IkarosLogoutSuccessHandler()));
    }
}
