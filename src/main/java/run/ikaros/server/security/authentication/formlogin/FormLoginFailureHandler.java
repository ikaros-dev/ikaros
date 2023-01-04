package run.ikaros.server.security.authentication.formlogin;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public class FormLoginFailureHandler implements ServerAuthenticationFailureHandler {
    private final ServerResponse.Context context;

    public FormLoginFailureHandler(ServerResponse.Context context) {
        this.context = context;
    }

    @Override
    public Mono<Void> onAuthenticationFailure(WebFilterExchange webFilterExchange,
                                              AuthenticationException exception) {
        return null;
    }
}
