package run.ikaros.server.security.authentication.oauth2;

import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.stereotype.Component;
import run.ikaros.server.security.authentication.SecurityConfigurer;

@Component
public class Oauth2Configurer implements SecurityConfigurer {
    @Override
    public void configure(ServerHttpSecurity http) {

    }
}
