package run.ikaros.server.security.authentication.formlogin;

import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.user.UserService;
import run.ikaros.server.infra.utils.JsonUtils;


/**
 * Return a user detail json when login success.
 *
 * @see <a href="https://stackoverflow.com/questions/45211431/webexceptionhandler-how-to-write-a-body-with-spring-webflux">WebExceptionHandler : How to write a body with Spring Webflux</a>
 */
@Component
public class FormLoginSuccessHandler implements ServerAuthenticationSuccessHandler {
    private final UserService userService;

    public FormLoginSuccessHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange,
                                              Authentication authentication) {
        User principal = (User) authentication.getPrincipal();
        String username = principal.getUsername();
        ServerHttpResponse response = webFilterExchange.getExchange().getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return userService.getUserByUsername(username)
            .flatMap(user -> Mono.justOrEmpty(JsonUtils.obj2Json(user)))
            .flatMap(json -> Mono.just(json.getBytes(StandardCharsets.UTF_8)))
            .flatMap(bytes -> Mono.just(response
                .bufferFactory().wrap(bytes)))
            .flatMap(dataBuffer -> response.writeWith(Flux.just(dataBuffer)));
    }
}
