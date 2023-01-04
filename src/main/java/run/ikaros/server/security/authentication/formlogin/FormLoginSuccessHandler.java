package run.ikaros.server.security.authentication.formlogin;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public class FormLoginSuccessHandler implements ServerAuthenticationSuccessHandler {
    private final ServerResponse.Context context;

    public FormLoginSuccessHandler(ServerResponse.Context context) {
        this.context = context;
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange,
                                              Authentication authentication) {
        return null;
    }
}
