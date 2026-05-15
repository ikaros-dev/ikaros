package run.ikaros.server.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.stereotype.Component;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.server.security.authentication.SecurityConfigurer;

@Component
public class CsrfConfigurer implements SecurityConfigurer {
    @Override
    public void configure(HttpSecurity http) {
        http.securityMatcher(SecurityConst.SECURITY_MATCHER_PATHS)
            .csrf(csrfConfigurer -> {
                csrfConfigurer.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
                csrfConfigurer.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler());
            });
    }
}