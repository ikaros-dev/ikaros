package run.ikaros.server.security.authorization;

import static run.ikaros.api.constant.OpenApiConst.CORE_VERSION;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.server.infra.utils.JsonUtils;

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

        authentication.map(auth -> {
            Set<String> authorities = auth.getAuthorities()
                .stream().map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
            if (authorities.isEmpty()) {
                return new AuthorizationDecision(false);
            }
            boolean granted = false;
            for (String authority : authorities) {
                String[] split = authority.split(SecurityConst.AUTHORITY_DIVIDE);
                if (split.length != 2) {
                    log.debug("Invalid authority: {}", authority);
                    granted = false;
                    break;
                }
                String target = split[0];
                String author = split[1];
                if ("*".equals(target) && "*".equals(author)) {
                    granted = true;
                    break;
                }
                if (!"*".equals(author)) {
                    String[] methods = JsonUtils.json2ObjArr(author, new TypeReference<>() {
                    });
                    assert methods != null;
                    granted = Set.of(methods).contains(method.name());
                }
                // todo 匹配路径和target
            }
            return new AuthorizationDecision(granted);
        });

        return authentication.map(auth -> new AuthorizationDecision(
            auth.getAuthorities()
                .contains(new SimpleGrantedAuthority(
                    SecurityConst.PREFIX + SecurityConst.ROLE_MASTER))));
    }

}
