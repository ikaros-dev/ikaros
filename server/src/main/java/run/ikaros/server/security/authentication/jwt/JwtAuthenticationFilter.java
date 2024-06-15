package run.ikaros.server.security.authentication.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JwtAuthenticationFilter implements WebFilter {
    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final ReactiveUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtAuthenticationProvider jwtAuthenticationProvider,
                                   ReactiveUserDetailsService userDetailsService
    ) {
        this.jwtAuthenticationProvider = jwtAuthenticationProvider;
        this.userDetailsService = userDetailsService;
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token =
            extractToken(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (token != null && jwtAuthenticationProvider.validateToken(token)) {
            String username = jwtAuthenticationProvider.extractUsername(token);
            return userDetailsService.findByUsername(username)
                .map(userDetails -> new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()))
                .map(ReactiveSecurityContextHolder::withAuthentication)
                .flatMap(context -> chain.filter(exchange).contextWrite(context));
        }
        return chain.filter(exchange);
    }

    private String extractToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
