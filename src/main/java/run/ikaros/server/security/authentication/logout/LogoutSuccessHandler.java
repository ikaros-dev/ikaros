package run.ikaros.server.security.authentication.logout;

import static run.ikaros.server.security.authentication.WebExchangeMatchers.ignoringMediaTypeAll;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.constant.AppConst;

public class LogoutSuccessHandler implements ServerLogoutSuccessHandler {

    private final ServerLogoutSuccessHandler defaultHandler;

    /**
     * logout success handler.
     */
    public LogoutSuccessHandler() {
        var defaultHandler = new RedirectServerLogoutSuccessHandler();
        defaultHandler.setLogoutSuccessUrl(URI.create(AppConst.LOGOUT_SUCCESS_LOCATION));
        this.defaultHandler = defaultHandler;
    }

    @Override
    public Mono<Void> onLogoutSuccess(WebFilterExchange exchange,
                                      Authentication authentication) {
        return ignoringMediaTypeAll(MediaType.APPLICATION_JSON).matches(exchange.getExchange())
            .flatMap(matchResult -> {
                if (matchResult.isMatch()) {
                    exchange.getExchange().getResponse().setStatusCode(HttpStatus.OK);
                    return Mono.empty();
                }
                return defaultHandler.onLogoutSuccess(exchange, authentication);
            });
    }
}