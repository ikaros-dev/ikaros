package run.ikaros.server.security.authentication.basicauth;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;
import run.ikaros.server.security.authentication.SecurityConfigurer;

@Component
public class BasicAuthenticationConfigurer implements SecurityConfigurer {
    private final IkarosBasicAuthenticationFilter ikarosBasicAuthenticationFilter;

    public BasicAuthenticationConfigurer(
        IkarosBasicAuthenticationFilter basicAuthenticationFilter) {
        this.ikarosBasicAuthenticationFilter = basicAuthenticationFilter;
    }

    @Override
    public void configure(HttpSecurity http) {
        http.httpBasic(Customizer.withDefaults())
            .addFilterAfter(ikarosBasicAuthenticationFilter, BasicAuthenticationFilter.class);
    }
}
