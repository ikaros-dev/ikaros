package run.ikaros.server.security.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.api.constant.SecurityConst.Authorization;
import run.ikaros.api.store.enums.AuthorityType;

class RequestAuthorizationManagerTest {

    private RequestAuthorizationManager manager;

    @BeforeEach
    void setUp() {
        manager = new RequestAuthorizationManager();
    }

    private Authentication createAuth(String... authorities) {
        var granted = new java.util.ArrayList<GrantedAuthority>();
        for (String auth : authorities) {
            granted.add(new SimpleGrantedAuthority(auth));
        }
        return new UsernamePasswordAuthenticationToken("user", "pass", granted);
    }

    private AuthorizationContext createContext(String path, HttpMethod method) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(URI.create(path));
        when(request.getMethod()).thenReturn(method);
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(exchange.getRequest()).thenReturn(request);
        return new AuthorizationContext(exchange, null);
    }

    private void verifyDecision(Mono<AuthorizationResult> result, boolean expected) {
        StepVerifier.create(result)
            .assertNext(r -> assertThat(r.isGranted()).isEqualTo(expected))
            .verifyComplete();
    }

    @Test
    void alwaysAllow_staticResource() {
        var auth = createAuth("anonymous");
        var ctx = createContext("/api/v1/static/index.js", HttpMethod.GET);
        verifyDecision(manager.authorize(Mono.just(auth), ctx), true);
    }

    @Test
    void alwaysAllow_jwtApply() {
        var auth = createAuth("anonymous");
        var ctx = createContext("/api/v1/security/auth/token/jwt/apply", HttpMethod.POST);
        verifyDecision(manager.authorize(Mono.just(auth), ctx), true);
    }

    @Test
    void alwaysAllow_jwtRefresh() {
        var auth = createAuth("anonymous");
        var ctx = createContext("/api/v1/security/auth/token/jwt/refresh", HttpMethod.POST);
        verifyDecision(manager.authorize(Mono.just(auth), ctx), true);
    }

    @Test
    void alwaysAllow_attachmentStream() {
        var auth = createAuth("anonymous");
        var ctx = createContext("/api/v1/attachment/stream/123", HttpMethod.GET);
        verifyDecision(manager.authorize(Mono.just(auth), ctx), true);
    }

    @Test
    void alwaysAllow_localFilePrefix() {
        var auth = createAuth("anonymous");
        var ctx = createContext("/driver/static/test.mp4", HttpMethod.GET);
        verifyDecision(manager.authorize(Mono.just(auth), ctx), true);
    }

    @Test
    void deny_emptyAuthorities() {
        var auth = createAuth();
        var ctx = createContext("/api/v1/subject/123", HttpMethod.GET);
        verifyDecision(manager.authorize(Mono.just(auth), ctx), false);
    }

    @Test
    void deny_anonymousUser() {
        var auth = createAuth("anonymous");
        var ctx = createContext("/api/v1/subject/123", HttpMethod.GET);
        verifyDecision(manager.authorize(Mono.just(auth), ctx), false);
    }

    @Test
    void allow_allAuthorityType_allTarget_allAuthority() {
        String authority = AuthorityType.ALL.name() + SecurityConst.AUTHORITY_DIVIDE
            + Authorization.Target.ALL + SecurityConst.AUTHORITY_DIVIDE
            + Authorization.Authority.ALL;
        var auth = createAuth(authority);
        var ctx = createContext("/api/v1/subject/123", HttpMethod.GET);
        verifyDecision(manager.authorize(Mono.just(auth), ctx), true);
    }

    @Test
    void allow_allAuthorityType_allTarget_httpAll() {
        String authority = AuthorityType.ALL.name() + SecurityConst.AUTHORITY_DIVIDE
            + Authorization.Target.ALL + SecurityConst.AUTHORITY_DIVIDE
            + Authorization.Authority.HTTP_ALL;
        var auth = createAuth(authority);
        var ctx = createContext("/api/v1/subject/123", HttpMethod.POST);
        verifyDecision(manager.authorize(Mono.just(auth), ctx), true);
    }

    @Test
    void allow_allAuthorityType_allTarget_httpGet() {
        String authority = AuthorityType.ALL.name() + SecurityConst.AUTHORITY_DIVIDE
            + Authorization.Target.ALL + SecurityConst.AUTHORITY_DIVIDE
            + Authorization.Authority.HTTP_GET;
        var auth = createAuth(authority);
        var ctx = createContext("/api/v1/subject/123", HttpMethod.GET);
        verifyDecision(manager.authorize(Mono.just(auth), ctx), true);
    }

    @Test
    void deny_allAuthorityType_allTarget_httpGet_wrongMethod() {
        String authority = AuthorityType.ALL.name() + SecurityConst.AUTHORITY_DIVIDE
            + Authorization.Target.ALL + SecurityConst.AUTHORITY_DIVIDE
            + Authorization.Authority.HTTP_GET;
        var auth = createAuth(authority);
        var ctx = createContext("/api/v1/subject/123", HttpMethod.POST);
        verifyDecision(manager.authorize(Mono.just(auth), ctx), false);
    }

    @Test
    void allow_apiAuthorityType_matchingTargetAndMethod() {
        String authority = AuthorityType.API.name() + SecurityConst.AUTHORITY_DIVIDE
            + Authorization.Target.API_CORE_SUBJECT + SecurityConst.AUTHORITY_DIVIDE
            + Authorization.Authority.HTTP_ALL;
        var auth = createAuth(authority);
        var ctx = createContext("/api/v1/subject/123", HttpMethod.GET);
        verifyDecision(manager.authorize(Mono.just(auth), ctx), true);
    }

    @Test
    void deny_apiAuthorityType_nonMatchingTarget() {
        String authority = AuthorityType.API.name() + SecurityConst.AUTHORITY_DIVIDE
            + Authorization.Target.API_CORE_ROLE + SecurityConst.AUTHORITY_DIVIDE
            + Authorization.Authority.HTTP_ALL;
        var auth = createAuth(authority);
        var ctx = createContext("/api/v1/subject/123", HttpMethod.GET);
        verifyDecision(manager.authorize(Mono.just(auth), ctx), false);
    }

    @Test
    void allow_apiAuthorityType_allTarget() {
        String authority = AuthorityType.API.name() + SecurityConst.AUTHORITY_DIVIDE
            + Authorization.Target.ALL + SecurityConst.AUTHORITY_DIVIDE
            + Authorization.Authority.HTTP_ALL;
        var auth = createAuth(authority);
        var ctx = createContext("/api/v1/subject/123", HttpMethod.GET);
        verifyDecision(manager.authorize(Mono.just(auth), ctx), true);
    }

    @Test
    void deny_invalidAuthorityFormat() {
        var auth = createAuth("invalid_authority_format");
        var ctx = createContext("/api/v1/subject/123", HttpMethod.GET);
        verifyDecision(manager.authorize(Mono.just(auth), ctx), false);
    }

    @Test
    void allow_apisAuthorityType_matchingTargetAndMethod() {
        String authority = AuthorityType.APIS.name() + SecurityConst.AUTHORITY_DIVIDE
            + Authorization.Target.API_CORE_SUBJECT + SecurityConst.AUTHORITY_DIVIDE
            + Authorization.Authority.HTTP_ALL;
        var auth = createAuth(authority);
        var ctx = createContext("/api/v1/subject/123", HttpMethod.GET);
        verifyDecision(manager.authorize(Mono.just(auth), ctx), true);
    }
}
