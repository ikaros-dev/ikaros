package run.ikaros.server.security;

import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.stereotype.Component;

@Component
public class DisableConfigurer implements SecurityConfigurer {
    @Override
    public void configure(ServerHttpSecurity http) {
        http
            .formLogin().disable()
            .logout().disable()
            .httpBasic().disable();
    }
}
