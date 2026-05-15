package run.ikaros.server.security.authorization;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.api.store.enums.AuthorityType;

class RequestAuthorizationManagerTest {

    private RequestAuthorizationManager manager;

    @BeforeEach
    void setUp() {
        manager = new RequestAuthorizationManager();
    }

    private Authentication mockAuth(Collection<? extends GrantedAuthority> authorities) {
        Authentication auth = mock(Authentication.class);
        when(auth.getAuthorities()).thenReturn((Collection) authorities);
        return auth;
    }

    private GrantedAuthority authority(String type, String target, String authority) {
        return () -> type + SecurityConst.AUTHORITY_DIVIDE
            + target + SecurityConst.AUTHORITY_DIVIDE
            + authority;
    }

    private RequestAuthorizationContext mockContext(String method, String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(uri);
        return new RequestAuthorizationContext(request, java.util.Map.of());
    }

    private AuthorizationDecision authorize(Supplier<Authentication> auth,
                                            RequestAuthorizationContext ctx) {
        AuthorizationResult result = manager.authorize(auth, ctx);
        return result instanceof AuthorizationDecision d ? d
            : new AuthorizationDecision(false);
    }

    // ===== Whitelist tests =====

    @Test
    void authorize_staticResourcePath_shouldBeGranted() {
        Supplier<Authentication> supplier = () -> null;
        RequestAuthorizationContext context = mockContext("GET",
            "/api/v1/static/images/test.png");

        AuthorizationDecision decision = authorize(supplier, context);
        assertTrue(decision.isGranted());
    }

    @Test
    void authorize_jwtApplyEndpoint_shouldBeGranted() {
        Supplier<Authentication> supplier = () -> null;
        RequestAuthorizationContext context = mockContext("POST",
            "/api/v1/security/auth/token/jwt/apply");

        AuthorizationDecision decision = authorize(supplier, context);
        assertTrue(decision.isGranted());
    }

    @Test
    void authorize_jwtRefreshEndpoint_shouldBeGranted() {
        Supplier<Authentication> supplier = () -> null;
        RequestAuthorizationContext context = mockContext("POST",
            "/api/v1/security/auth/token/jwt/refresh");

        AuthorizationDecision decision = authorize(supplier, context);
        assertTrue(decision.isGranted());
    }

    @Test
    void authorize_attachmentStreamPath_shouldBeGranted() {
        Supplier<Authentication> supplier = () -> null;
        RequestAuthorizationContext context = mockContext("GET",
            "/api/v1/attachment/stream/some-file");

        AuthorizationDecision decision = authorize(supplier, context);
        assertTrue(decision.isGranted());
    }

    @Test
    void authorize_nonWhitelistedPath_shouldNotBeGrantedWithoutAuth() {
        Supplier<Authentication> supplier = () -> null;
        RequestAuthorizationContext context = mockContext("GET",
            "/api/v1/user/me");

        AuthorizationDecision decision = authorize(supplier, context);
        assertFalse(decision.isGranted());
    }

    // ===== Null/empty authentication tests =====

    @Test
    void authorize_nullAuth_shouldNotBeGranted() {
        Supplier<Authentication> supplier = () -> null;
        RequestAuthorizationContext context = mockContext("GET",
            "/api/v1/user/me");

        AuthorizationDecision decision = authorize(supplier, context);
        assertFalse(decision.isGranted());
    }

    @Test
    void authorize_emptyAuthorities_shouldNotBeGranted() {
        Authentication auth = mockAuth(List.of());
        Supplier<Authentication> supplier = () -> auth;
        RequestAuthorizationContext context = mockContext("GET",
            "/api/v1/user/me");

        AuthorizationDecision decision = authorize(supplier, context);
        assertFalse(decision.isGranted());
    }

    @Test
    void authorize_anonymousUser_shouldNotBeGranted() {
        Authentication auth = mockAuth(List.of(
            () -> "anonymous"
        ));
        Supplier<Authentication> supplier = () -> auth;
        RequestAuthorizationContext context = mockContext("GET",
            "/api/v1/user/me");

        AuthorizationDecision decision = authorize(supplier, context);
        assertFalse(decision.isGranted());
    }

    // ===== ALL authority tests =====

    @Test
    void authorize_allStarStar_shouldGrantAllPaths() {
        Authentication auth = mockAuth(List.of(
            authority(AuthorityType.ALL.name(), "*", "*")
        ));
        Supplier<Authentication> supplier = () -> auth;
        RequestAuthorizationContext context = mockContext("GET",
            "/api/v1/user/me");

        AuthorizationDecision decision = authorize(supplier, context);
        assertTrue(decision.isGranted());
    }

    @Test
    void authorize_allStarHttpAll_shouldGrantAllMethods() {
        Authentication auth = mockAuth(List.of(
            authority(AuthorityType.ALL.name(), "*", "HTTP_**")
        ));
        Supplier<Authentication> supplier = () -> auth;
        RequestAuthorizationContext context = mockContext("POST",
            "/api/v1/some/path");

        AuthorizationDecision decision = authorize(supplier, context);
        assertTrue(decision.isGranted());
    }

    @Test
    void authorize_allStarHttpGet_shouldGrantGetOnly() {
        Authentication auth = mockAuth(List.of(
            authority(AuthorityType.ALL.name(), "*", "HTTP_GET")
        ));
        Supplier<Authentication> supplier = () -> auth;

        RequestAuthorizationContext getCtx = mockContext("GET", "/api/v1/anything");
        assertTrue(authorize(supplier, getCtx).isGranted());

        RequestAuthorizationContext postCtx = mockContext("POST", "/api/v1/anything");
        assertFalse(authorize(supplier, postCtx).isGranted());
    }

    // ===== API authority tests =====

    @Test
    void authorize_apiTargetWithAllAuthority_shouldGrant() {
        Authentication auth = mockAuth(List.of(
            authority(AuthorityType.API.name(),
                "/api/v1/user/**", "*")
        ));
        Supplier<Authentication> supplier = () -> auth;
        RequestAuthorizationContext context = mockContext("GET",
            "/api/v1/user/me");

        AuthorizationDecision decision = authorize(supplier, context);
        assertTrue(decision.isGranted());
    }

    @Test
    void authorize_apiTargetWithMatchingMethod_shouldGrant() {
        Authentication auth = mockAuth(List.of(
            authority(AuthorityType.API.name(),
                "/api/v1/user/**", "HTTP_GET")
        ));
        Supplier<Authentication> supplier = () -> auth;
        RequestAuthorizationContext context = mockContext("GET",
            "/api/v1/user/me");

        AuthorizationDecision decision = authorize(supplier, context);
        assertTrue(decision.isGranted());
    }

    @Test
    void authorize_apiTargetWithNonMatchingMethod_shouldNotGrant() {
        Authentication auth = mockAuth(List.of(
            authority(AuthorityType.API.name(),
                "/api/v1/user/**", "HTTP_GET")
        ));
        Supplier<Authentication> supplier = () -> auth;
        RequestAuthorizationContext context = mockContext("DELETE",
            "/api/v1/user/me");

        AuthorizationDecision decision = authorize(supplier, context);
        assertFalse(decision.isGranted());
    }

    @Test
    void authorize_apiTargetNotMatchingPath_shouldNotGrant() {
        Authentication auth = mockAuth(List.of(
            authority(AuthorityType.API.name(),
                "/api/v1/subject/**", "*")
        ));
        Supplier<Authentication> supplier = () -> auth;
        RequestAuthorizationContext context = mockContext("GET",
            "/api/v1/user/me");

        AuthorizationDecision decision = authorize(supplier, context);
        assertFalse(decision.isGranted());
    }

    @Test
    void authorize_apisTargetCustom_shouldGrant() {
        Authentication auth = mockAuth(List.of(
            authority(AuthorityType.APIS.name(),
                "/apis/**", "*")
        ));
        Supplier<Authentication> supplier = () -> auth;
        RequestAuthorizationContext context = mockContext("GET",
            "/apis/custom/endpoint");

        AuthorizationDecision decision = authorize(supplier, context);
        assertTrue(decision.isGranted());
    }

    // ===== Invalid authority format tests =====

    @Test
    void authorize_invalidAuthorityFormat_shouldNotGrant() {
        Authentication auth = mockAuth(List.of(
            () -> "invalid-authority-without-dividers"
        ));
        Supplier<Authentication> supplier = () -> auth;
        RequestAuthorizationContext context = mockContext("GET",
            "/api/v1/user/me");

        AuthorizationDecision decision = authorize(supplier, context);
        assertFalse(decision.isGranted());
    }

    // ===== HTTP_** wildcard tests =====

    @Test
    void authorize_httpAllWildcard_shouldGrantAnyMethod() {
        Authentication auth = mockAuth(List.of(
            authority(AuthorityType.API.name(),
                "/api/v1/user/**", "HTTP_**")
        ));
        Supplier<Authentication> supplier = () -> auth;

        assertTrue(authorize(supplier,
            mockContext("GET", "/api/v1/user/me")).isGranted());
        assertTrue(authorize(supplier,
            mockContext("POST", "/api/v1/user/me")).isGranted());
        assertTrue(authorize(supplier,
            mockContext("DELETE", "/api/v1/user/me")).isGranted());
    }
}
