package run.ikaros.server.security.authorization;

import static run.ikaros.api.constant.OpenApiConst.CORE_VERSION;
import static run.ikaros.api.core.attachment.AttachmentConst.DRIVER_STATIC_RESOURCE_PREFIX;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
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

    private boolean authTarget(String target, String path, boolean granted) {
        if (!granted && Authorization.Target.ALL.equals(target)) {
            return true;
        }

        if (target.contains("/**")) {
            String apiPrefix = target.substring(0, target.lastIndexOf("/**"));
            if (!granted && path.contains(apiPrefix)) {
                return true;
            }


        } else {
            if (!granted && path.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return granted;
    }

    private boolean authMethod(String author, HttpMethod method, boolean granted) {
        if (!granted && Authorization.Authority.ALL.equals(author)) {
            return true;
        }
        if (!author.contains("HTTP")) {
            return granted;
        }
        if (!granted && author.contains("_**")) {
            return true;
        }
        if (!granted && author.contains(method.name())) {
            return true;
        }
        return granted;
    }

    @Override
    public Mono<AuthorizationResult> authorize(Mono<Authentication> authentication,
                                               AuthorizationContext object) {
        final ServerHttpRequest request = object.getExchange().getRequest();
        final String path = request.getURI().getPath();
        final HttpMethod method = request.getMethod();
        if (path.startsWith("/api/" + CORE_VERSION + "/static/")
            || path.equals("/api/" + CORE_VERSION + "/security/auth/token/jwt/apply")
            || path.equals("/api/" + CORE_VERSION + "/security/auth/token/jwt/refresh")
            || path.startsWith("/api/" + CORE_VERSION + "/attachment/stream")
            || path.startsWith(DRIVER_STATIC_RESOURCE_PREFIX + '/') // todo 后续可能考虑对本地映射文件鉴权
        ) {
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
                        continue;
                    }

                    if (Authorization.Target.ALL.equals(target)
                        && Authorization.Authority.HTTP_ALL.equals(author)) {
                        granted = true;
                        continue;
                    }

                    if (!Authorization.Authority.ALL.equals(author) && author.startsWith("HTTP")) {
                        if (author.contains(method.name())) {
                            granted = true;
                            continue;
                        }
                    }
                }

                if (AuthorityType.API.equals(type) || AuthorityType.APIS.equals(type)) {

                    if (authTarget(target, path, granted)
                        && authMethod(author, method, granted)) {
                        granted = true;
                        continue;
                    }
                }

                // todo 匹配其它权限类型


                if (granted) {
                    break;
                }
            }
            return new AuthorizationDecision(granted);
        });
    }
}
