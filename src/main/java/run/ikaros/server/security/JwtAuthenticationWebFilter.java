package run.ikaros.server.security;

import static run.ikaros.server.infra.constant.SecurityConst.API_CORE_MATCHER;
import static run.ikaros.server.infra.constant.SecurityConst.API_PLUGIN_MATCHER;
import static run.ikaros.server.infra.constant.SecurityConst.TOKEN_HEADER;

import java.util.function.Function;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import run.ikaros.server.infra.util.JwtUtils;

/**
 * convert header jwt token to Authentication, and save to spring security context.
 *
 * @see Authentication
 * @see JwtAuthenticationConfigurer
 */

public class JwtAuthenticationWebFilter implements WebFilter {

    private ServerWebExchangeMatcher requiresAuthenticationMatcher
        = ServerWebExchangeMatchers.pathMatchers(API_CORE_MATCHER, API_PLUGIN_MATCHER);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return this.requiresAuthenticationMatcher.matches(exchange)
            .filter(ServerWebExchangeMatcher.MatchResult::isMatch)
            .flatMap(matchResult -> getHttpHeaderValues(exchange))
            .flatMap(token -> Mono.just(JwtUtils.getAuthentication(token)))
            .map(JwtAuthenticationWebFilter::setAuthentication2Context)
            .checkpoint("update context authentication")
            .then(chain.filter(exchange));
    }

    private Mono<String> getHttpHeaderValues(ServerWebExchange exchange) {
        return Mono.justOrEmpty(
                exchange.getRequest().getHeaders().get(TOKEN_HEADER))
            .switchIfEmpty(
                Mono.error(new JwtAuthenticationException("no token in header " + TOKEN_HEADER)))
            .flatMap(strings -> Mono.just(strings.get(0)))
            .checkpoint("get http header value" + TOKEN_HEADER);
    }

    private static Authentication setAuthentication2Context(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }
}
