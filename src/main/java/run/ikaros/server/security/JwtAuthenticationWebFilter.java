package run.ikaros.server.security;

import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * convert header jwt token to Authentication, and save to spring security context.
 *
 * @see Authentication
 * @see JwtAuthenticationConfigurer
 */
public class JwtAuthenticationWebFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return null;
    }
}
