package run.ikaros.server.security.authorization;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.constant.SecurityConst;

@Slf4j
public class RequestAuthorizationManager
    implements ReactiveAuthorizationManager<AuthorizationContext> {
    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication,
                                             AuthorizationContext object) {
        boolean urlStartWithApiStatic =
            object.getExchange().getRequest().getURI().getPath()
                .startsWith("/api/" + OpenApiConst.CORE_VERSION + "/static/");
        if (urlStartWithApiStatic) {
            return authentication.map(auth -> new AuthorizationDecision(true));
        }
        return authentication.map(auth -> new AuthorizationDecision(
            auth.getAuthorities()
                .contains(new SimpleGrantedAuthority(
                    SecurityConst.PREFIX + SecurityConst.ROLE_MASTER))));
    }

}
