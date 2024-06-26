package run.ikaros.server.security.authorization;

import static run.ikaros.api.constant.OpenApiConst.CORE_VERSION;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.api.constant.SecurityConst.Authorization;
import run.ikaros.api.store.enums.AuthorityType;

@Slf4j
public class RequestAuthorizationManager
    implements ReactiveAuthorizationManager<AuthorizationContext> {
    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication,
                                             AuthorizationContext object) {
        final ServerHttpRequest request = object.getExchange().getRequest();
        final String path = request.getURI().getPath();
        final HttpMethod method = request.getMethod();
        boolean urlStartWithApiStatic = path
            .startsWith("/api/" + CORE_VERSION + "/static/");
        if (urlStartWithApiStatic) {
            return authentication.map(auth -> new AuthorizationDecision(true));
        }

        if (path.equals("/api/" + CORE_VERSION + "/security/auth/token/jwt/apply")) {
            return authentication.map(auth -> new AuthorizationDecision(true));
        }

        return authentication.map(auth -> {
            Set<String> authorities = auth.getAuthorities()
                .stream().map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
            if (authorities.isEmpty()) {
                return new AuthorizationDecision(false);
            }

            if (authorities.size() == 1 && authorities.toArray()[0] == "anonymous") {
                return new AuthorizationDecision(false);
            }

            boolean granted = false;

            for (String authority : authorities) {

                String[] split = authority.split(SecurityConst.AUTHORITY_DIVIDE);
                if (split.length != 3) {
                    log.debug("Invalid authority: {}", authority);
                    granted = false;
                    break;
                }
                AuthorityType type = AuthorityType.valueOf(split[0]);
                String target = split[1];
                String author = split[2];

                if (AuthorityType.ALL.equals(type)) {
                    if (Authorization.Target.ALL.equals(target)
                        && Authorization.Authority.ALL.equals(author)) {
                        granted = true;
                        break;
                    }

                    if (Authorization.Target.ALL.equals(target)
                        && Authorization.Authority.HTTP_ALL.equals(author)) {
                        granted = true;
                        break;
                    }

                    if (!Authorization.Authority.ALL.equals(author) && author.startsWith("HTTP")) {
                        granted = author.contains(method.name());
                    }
                }

                // todo 匹配其它权限类型
            }
            return new AuthorizationDecision(granted);
        });
    }

}
