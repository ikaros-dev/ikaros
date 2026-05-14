package run.ikaros.server.security.authentication.jwt;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;
import run.ikaros.server.security.authentication.SecurityConfigurer;

@Component
public class JwtAuthenticationConfigurer implements SecurityConfigurer {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationManager jwtAuthenticationManager;

    public JwtAuthenticationConfigurer(
        JwtAuthenticationFilter jwtAuthenticationFilter,
        JwtAuthenticationManager jwtAuthenticationManager) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationManager = jwtAuthenticationManager;
    }

    @Override
    public void configure(HttpSecurity http) {
        http.authenticationManager(jwtAuthenticationManager)
            .addFilterAfter(jwtAuthenticationFilter, BasicAuthenticationFilter.class);
    }
}
