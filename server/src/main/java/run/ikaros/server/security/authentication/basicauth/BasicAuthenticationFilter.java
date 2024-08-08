package run.ikaros.server.security.authentication.basicauth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class BasicAuthenticationFilter implements WebFilter {
    private final ReactiveUserDetailsService userDetailsService;

    public BasicAuthenticationFilter(ReactiveUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String basic64 =
            extractBasic64(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (basic64 != null) {
            String decoder =
                new String(Base64.getDecoder().decode(basic64), StandardCharsets.UTF_8);
            if (!decoder.contains(":")) {
                return Mono.empty();
            }
            String[] split = decoder.split(":");
            String username = split[0];
            String password = split[1];
            return userDetailsService.findByUsername(username)
                .map(userDetails -> new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()))
                .map(authenticationToken -> {
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    return authenticationToken;
                })
                .map(ReactiveSecurityContextHolder::withAuthentication)
                .flatMap(context -> chain.filter(exchange).contextWrite(context));
        }
        return chain.filter(exchange);
    }

    private String extractBasic64(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Basic ")) {
            return bearerToken.substring(6);
        }
        return null;
    }
}
