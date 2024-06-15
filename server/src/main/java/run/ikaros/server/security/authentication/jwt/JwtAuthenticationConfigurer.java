package run.ikaros.server.security.authentication.jwt;

import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.stereotype.Component;
import run.ikaros.server.security.authentication.SecurityConfigurer;

@Component
public class JwtAuthenticationConfigurer implements SecurityConfigurer {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtReactiveAuthenticationManager jwtAuthenticationManager;

    public JwtAuthenticationConfigurer(
        JwtAuthenticationFilter jwtAuthenticationFilter,
        JwtReactiveAuthenticationManager jwtAuthenticationManager) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationManager = jwtAuthenticationManager;
    }

    @Override
    public void configure(ServerHttpSecurity http) {
        http.authenticationManager(jwtAuthenticationManager)
            .addFilterAfter(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION);
    }
}
