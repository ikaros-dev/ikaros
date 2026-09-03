package run.ikaros.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.util.context.Context;
import run.ikaros.identity.AccessControlService;
import reactor.core.publisher.Mono;

class ResourceAuthorizationWebFilterTest {
    @Test
    void rejectsResourceRequestWithoutSession() {
        UUID actor = UUID.randomUUID();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/resources")
            .header("X-Ikaros-Actor-Id", actor.toString()).build());
        WebFilterChain chain = mock(WebFilterChain.class);
        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();
        assertEquals(401, exchange.getResponse().getStatusCode().value());
        assertEquals("application/problem+json", exchange.getResponse().getHeaders().getFirst("Content-Type"));
    }

    @Test
    void rejectsDeliveryAdminRequestWithoutSession() {
        UUID actor = UUID.randomUUID();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(
            "/api/admin/delivery-providers").header("X-Ikaros-Actor-Id", actor.toString()).build());
        WebFilterChain chain = mock(WebFilterChain.class);

        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void rejectsStorageProviderAdminAliasWithoutSession() {
        UUID actor = UUID.randomUUID();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(
            "/api/admin/storage-providers").header("X-Ikaros-Actor-Id", actor.toString()).build());
        WebFilterChain chain = mock(WebFilterChain.class);

        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void rejectsRestoreRequestWithoutSession() {
        UUID actor = UUID.randomUUID();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post(
            "/api/attachments/" + UUID.randomUUID() + "/restore-requests")
            .header("X-Ikaros-Actor-Id", actor.toString()).build());
        WebFilterChain chain = mock(WebFilterChain.class);

        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void rejectsDeliveryGrantWithoutSession() {
        UUID actor = UUID.randomUUID();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post(
            "/api/attachments/" + UUID.randomUUID() + "/delivery-grants")
            .header("X-Ikaros-Actor-Id", actor.toString()).build());
        WebFilterChain chain = mock(WebFilterChain.class);

        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void rejectsBlobPlacementAdminQueryWithoutSession() {
        UUID actor = UUID.randomUUID();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(
            "/api/admin/blobs/" + UUID.randomUUID() + "/placements")
            .header("X-Ikaros-Actor-Id", actor.toString()).build());
        WebFilterChain chain = mock(WebFilterChain.class);

        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void rejectsIdentityAdministrationWithoutSession() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/admin/users")
            .header("X-Ikaros-Actor-Id", UUID.randomUUID().toString()).build());
        WebFilterChain chain = mock(WebFilterChain.class);

        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void rejectsBackupAdministrationWithoutSession() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(
            "/api/admin/backup/restore-points").build());
        WebFilterChain chain = mock(WebFilterChain.class);

        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void rejectsUnlistedApiRouteWithoutSession() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(
            "/api/finance/ledgers").build());
        WebFilterChain chain = mock(WebFilterChain.class);

        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void letsDeliveryGrantContentReachGrantAuthorizationWithoutSession() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(
            "/api/attachments/" + UUID.randomUUID() + "/content")
            .header("X-Ikaros-Delivery-Grant", "opaque-grant").build());
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();

        assertEquals(null, exchange.getResponse().getStatusCode());
    }

    @Test
    void requiresFreshStepUpForRolePermissionChanges() {
        UUID actor = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post(
            "/api/admin/roles/" + UUID.randomUUID() + "/permissions/SYSTEM_ROLE_READ")
            .build());
        WebFilterChain chain = mock(WebFilterChain.class);
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.require(any(), any(), any())).thenReturn(Mono.empty());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        new ResourceAuthorizationWebFilter(accessControl).filter(exchange, chain)
            .contextWrite(Context.of(PrincipalContext.CONTEXT_KEY,
                new PrincipalContext(actor, session, "request", "correlation", false)))
            .block();

        verify(accessControl).require(actor, session,
            new run.ikaros.identity.SecurityPolicy("resource.http",
                run.ikaros.identity.PlatformPermission.SYSTEM_ROLE_MANAGE,
                run.ikaros.identity.SecurityVerificationLevel.SVL_2, true));
    }

    @Test
    void doesNotTreatArbitraryContentPathAsGrantOnlyDelivery() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(
            "/api/resources/" + UUID.randomUUID() + "/content")
            .header("X-Ikaros-Delivery-Grant", "opaque-grant").build());
        WebFilterChain chain = mock(WebFilterChain.class);

        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }
}
