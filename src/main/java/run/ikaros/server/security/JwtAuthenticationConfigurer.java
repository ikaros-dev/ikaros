package run.ikaros.server.security;

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
            // authentication
            .authenticationManager(new JwtReactiveAuthenticationManager())
            // authorization
            .authorizeExchange().pathMatchers("/api/**", "/apis/**")
            .access((authentication, object) -> Mono.just(new AuthorizationDecision(true)))
            .and()
            .addFilterBefore(new JwtAuthenticationWebFilter(),
                SecurityWebFiltersOrder.AUTHENTICATION)
        ;
    }
}
