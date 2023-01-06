package run.ikaros.server.security.authentication.formlogin;

import static run.ikaros.server.security.authentication.WebExchangeMatchers.ignoringMediaTypeAll;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.server.infra.constant.AppConst;

public class FormLoginFailureHandler implements ServerAuthenticationFailureHandler {
    private final ServerResponse.Context context;
    private final ServerAuthenticationFailureHandler defaultHandler =
        new RedirectServerAuthenticationFailureHandler(AppConst.LOGIN_FAILURE_LOCATION);

    public FormLoginFailureHandler(ServerResponse.Context context) {
        this.context = context;
    }

    @Override
    public Mono<Void> onAuthenticationFailure(WebFilterExchange webFilterExchange,
                                              AuthenticationException exception) {
        return ignoringMediaTypeAll(MediaType.APPLICATION_JSON).matches(
                webFilterExchange.getExchange())
            .flatMap(matchResult -> {
                if (matchResult.isMatch()) {
                    return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of(
                            "templates/error", exception.getLocalizedMessage()
                        ))
                        .flatMap(serverResponse -> serverResponse.writeTo(
                            webFilterExchange.getExchange(), context));
                }
                return defaultHandler.onAuthenticationFailure(webFilterExchange, exception);
            });
    }
}
