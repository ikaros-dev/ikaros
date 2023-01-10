package run.ikaros.server.security.authentication.formlogin;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import reactor.core.publisher.Mono;
import run.ikaros.server.infra.constant.AppConst;

public class FormLoginSuccessHandler implements ServerAuthenticationSuccessHandler {
    private final ServerAuthenticationSuccessHandler defaultHandler =
        new RedirectServerAuthenticationSuccessHandler(AppConst.LOGIN_SUCCESS_LOCATION);

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange,
                                              Authentication authentication) {
        return defaultHandler.onAuthenticationSuccess(webFilterExchange, authentication);
    }
}
