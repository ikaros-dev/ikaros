package run.ikaros.server.security.authentication.jwt;

import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.stereotype.Component;
import run.ikaros.server.security.authentication.SecurityConfigurer;

/**
 * JWT token authentication config.
 *
 * @author: li-guohao
 */
@Component
public class JwtAuthenticationConfigurer implements SecurityConfigurer {
    @Override
    public void configure(ServerHttpSecurity http) {

    }
}
