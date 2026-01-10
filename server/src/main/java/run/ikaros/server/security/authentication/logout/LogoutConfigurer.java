package run.ikaros.server.security.authentication.logout;


import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.stereotype.Component;
import run.ikaros.server.security.authentication.SecurityConfigurer;

@Component
public class LogoutConfigurer implements SecurityConfigurer {

    @Override
    public void configure(ServerHttpSecurity http) {
        http.logout(logoutSpec ->
            logoutSpec.logoutSuccessHandler(new LogoutSuccessHandler()));
    }
}
