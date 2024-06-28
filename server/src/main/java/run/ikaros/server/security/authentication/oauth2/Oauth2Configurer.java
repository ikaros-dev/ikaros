package run.ikaros.server.security.authentication.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.stereotype.Component;
import run.ikaros.server.security.authentication.SecurityConfigurer;

@Slf4j
@Component
public class Oauth2Configurer implements SecurityConfigurer {
    @Override
    public void configure(ServerHttpSecurity http) {

    }
}
