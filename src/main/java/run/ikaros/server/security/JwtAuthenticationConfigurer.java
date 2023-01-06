package run.ikaros.server.security;

import static run.ikaros.server.infra.constant.SecurityConst.API_CORE_MATCHER;
import static run.ikaros.server.infra.constant.SecurityConst.API_PLUGIN_MATCHER;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationConfigurer implements SecurityConfigurer {
    @Override
    public void configure(ServerHttpSecurity http) {
        http
            .addFilterBefore(new JwtAuthenticationWebFilter(),
                SecurityWebFiltersOrder.AUTHENTICATION)
            // authentication
            .authenticationManager(new JwtReactiveAuthenticationManager())
            // authorization
            .authorizeExchange().pathMatchers(API_CORE_MATCHER, API_PLUGIN_MATCHER)
            .access((authentication, object) -> Mono.just(new AuthorizationDecision(true)))
        ;
    }
}
