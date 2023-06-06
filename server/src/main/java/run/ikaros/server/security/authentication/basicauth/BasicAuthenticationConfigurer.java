package run.ikaros.server.security.authentication.basicauth;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.stereotype.Component;
import run.ikaros.server.security.authentication.SecurityConfigurer;

@Component
public class BasicAuthenticationConfigurer implements SecurityConfigurer {
    @Override
    public void configure(ServerHttpSecurity http) {
        http.httpBasic(Customizer.withDefaults());
    }
}
