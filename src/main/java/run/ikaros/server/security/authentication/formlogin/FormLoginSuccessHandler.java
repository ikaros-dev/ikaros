package run.ikaros.server.security.authentication.formlogin;

import static run.ikaros.server.security.authentication.WebExchangeMatchers.ignoringMediaTypeAll;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.server.infra.constant.AppConst;

public class FormLoginSuccessHandler implements ServerAuthenticationSuccessHandler {
    private final ServerResponse.Context context;
    private final ServerAuthenticationSuccessHandler defaultHandler =
        new RedirectServerAuthenticationSuccessHandler(AppConst.LOGIN_SUCCESS_LOCATION);

    public FormLoginSuccessHandler(ServerResponse.Context context) {
        this.context = context;
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange,
                                              Authentication authentication) {
        return ignoringMediaTypeAll(MediaType.APPLICATION_JSON)
            .matches(webFilterExchange.getExchange())
            .flatMap(matchResult -> {
                if (matchResult.isMatch()) {
                    var principal = authentication.getPrincipal();
                    if (principal instanceof CredentialsContainer credentialsContainer) {
                        // erase sensitive data
                        credentialsContainer.eraseCredentials();
                    }
                    return ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(principal)
                        .flatMap(serverResponse ->
                            serverResponse.writeTo(webFilterExchange.getExchange(), context));
                }
                return defaultHandler.onAuthenticationSuccess(webFilterExchange,
                    authentication);
            });
    }
}
