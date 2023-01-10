package run.ikaros.server.security.authentication.formlogin;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import reactor.core.publisher.Mono;
import run.ikaros.server.infra.constant.AppConst;

public class FormLoginFailureHandler implements ServerAuthenticationFailureHandler {
    private final ServerAuthenticationFailureHandler defaultHandler =
        new RedirectServerAuthenticationFailureHandler(AppConst.LOGIN_FAILURE_LOCATION);

    @Override
    public Mono<Void> onAuthenticationFailure(WebFilterExchange webFilterExchange,
                                              AuthenticationException exception) {
        return defaultHandler.onAuthenticationFailure(webFilterExchange, exception);
    }
}
