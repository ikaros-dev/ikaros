package run.ikaros.server.security.authorization;

import static run.ikaros.api.constant.OpenApiConst.CORE_VERSION;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.SecurityConst;

@Slf4j
public class RequestAuthorizationManager
    implements ReactiveAuthorizationManager<AuthorizationContext> {
    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication,
                                             AuthorizationContext object) {
        final String path = object.getExchange().getRequest().getURI().getPath();
        boolean urlStartWithApiStatic = path
            .startsWith("/api/" + CORE_VERSION + "/static/");
        if (urlStartWithApiStatic) {
            return authentication.map(auth -> new AuthorizationDecision(true));
        }

        if (path.equals("/api/" + CORE_VERSION + "/security/auth/token/jwt/apply")) {
            return authentication.map(auth -> new AuthorizationDecision(true));
        }

        return authentication.map(auth -> new AuthorizationDecision(
            auth.getAuthorities()
                .contains(new SimpleGrantedAuthority(
                    SecurityConst.PREFIX + SecurityConst.ROLE_MASTER))));
    }

}
