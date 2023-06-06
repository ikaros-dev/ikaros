package run.ikaros.server.security;

import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfWebFilter;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.stereotype.Component;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.server.security.authentication.SecurityConfigurer;

@Component
public class CsrfConfigurer implements SecurityConfigurer {
    @Override
    public void configure(ServerHttpSecurity http) {
        var csrfMatcher = new AndServerWebExchangeMatcher(
            CsrfWebFilter.DEFAULT_CSRF_MATCHER,
            new NegatedServerWebExchangeMatcher(
                pathMatchers(SecurityConst.SECURITY_MATCHER_PATHS)));

        http.csrf().csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
            .csrfTokenRequestHandler(new ServerCsrfTokenRequestAttributeHandler())
            .requireCsrfProtectionMatcher(csrfMatcher);
    }
}
