package run.ikaros.server.security.authentication.logout;

import java.net.URI;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import reactor.core.publisher.Mono;
import run.ikaros.server.infra.constant.AppConst;

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
        return defaultHandler.onLogoutSuccess(exchange, authentication);
    }
}
