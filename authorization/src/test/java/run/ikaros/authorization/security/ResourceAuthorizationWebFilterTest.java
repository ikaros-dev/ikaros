package run.ikaros.authorization.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import run.ikaros.authorization.AccessControlService;
import run.ikaros.authentication.api.AuthenticatedPrincipal;
import reactor.core.publisher.Mono;

class ResourceAuthorizationWebFilterTest {
    @Test
    void rejectsResourceRequestWithoutToken() {
        UUID actor = UUID.randomUUID();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/resources")
            .header("X-Ikaros-Actor-Id", actor.toString()).build());
        WebFilterChain chain = mock(WebFilterChain.class);
        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();
        assertEquals(401, exchange.getResponse().getStatusCode().value());
        assertEquals("application/problem+json", exchange.getResponse().getHeaders().getFirst("Content-Type"));
    }

    @Test
    void rejectsDeliveryAdminRequestWithoutToken() {
        UUID actor = UUID.randomUUID();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(
            "/api/admin/delivery-providers").header("X-Ikaros-Actor-Id", actor.toString()).build());
        WebFilterChain chain = mock(WebFilterChain.class);

        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void rejectsStorageProviderAdminAliasWithoutToken() {
        UUID actor = UUID.randomUUID();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(
            "/api/admin/storage-providers").header("X-Ikaros-Actor-Id", actor.toString()).build());
        WebFilterChain chain = mock(WebFilterChain.class);

        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void rejectsRestoreRequestWithoutToken() {
        UUID actor = UUID.randomUUID();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post(
            "/api/attachments/" + UUID.randomUUID() + "/restore-requests")
            .header("X-Ikaros-Actor-Id", actor.toString()).build());
        WebFilterChain chain = mock(WebFilterChain.class);

        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void rejectsDeliveryGrantWithoutToken() {
        UUID actor = UUID.randomUUID();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post(
            "/api/attachments/" + UUID.randomUUID() + "/delivery-grants")
            .header("X-Ikaros-Actor-Id", actor.toString()).build());
        WebFilterChain chain = mock(WebFilterChain.class);

        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void rejectsBlobPlacementAdminQueryWithoutToken() {
        UUID actor = UUID.randomUUID();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(
            "/api/admin/blobs/" + UUID.randomUUID() + "/placements")
            .header("X-Ikaros-Actor-Id", actor.toString()).build());
        WebFilterChain chain = mock(WebFilterChain.class);

        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void rejectsIdentityAdministrationWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/admin/users")
            .header("X-Ikaros-Actor-Id", UUID.randomUUID().toString()).build());
        WebFilterChain chain = mock(WebFilterChain.class);

        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void rejectsBackupAdministrationWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(
            "/api/admin/backup/restore-points").build());
        WebFilterChain chain = mock(WebFilterChain.class);

        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void rejectsUnlistedApiRouteWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(
            "/api/finance/ledgers").build());
        WebFilterChain chain = mock(WebFilterChain.class);

        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void letsDeliveryGrantContentReachGrantAuthorizationWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(
            "/api/attachments/" + UUID.randomUUID() + "/content")
            .header("X-Ikaros-Delivery-Grant", "opaque-grant").build());
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();

        assertEquals(null, exchange.getResponse().getStatusCode());
    }

    @Test
    void authorizesRolePermissionChangesFromJwtClaims() {
        UUID actor = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post(
            "/api/admin/roles/" + UUID.randomUUID() + "/permissions/SYSTEM_ROLE_READ")
            .build());
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());
        exchange.getAttributes().put(AuthenticatedPrincipal.EXCHANGE_ATTRIBUTE,
            new AuthenticatedPrincipal(actor, session, 0L, java.util.List.of("system.role.manage")));

        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange, chain).block();
        verify(chain).filter(exchange);
    }

    @Test
    void requiresFreshVerificationForHighRiskRoleMutation() {
        UUID actor = UUID.randomUUID();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post(
            "/api/admin/roles/" + UUID.randomUUID() + "/permissions/SYSTEM_ROLE_READ").build());
        exchange.getAttributes().put(AuthenticatedPrincipal.EXCHANGE_ATTRIBUTE,
            new AuthenticatedPrincipal(actor, UUID.randomUUID(), 0L, java.util.List.of("system.role.manage")));
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.require(eq(actor), eq(run.ikaros.authentication.api.SecurityVerificationLevel.SVL_0),
            eq(null), any())).thenReturn(Mono.error(new run.ikaros.common.ForbiddenException("step-up required")));

        new ResourceAuthorizationWebFilter(accessControl).filter(exchange, chain).block();

        assertEquals(403, exchange.getResponse().getStatusCode().value());
        org.mockito.Mockito.verify(chain, org.mockito.Mockito.never()).filter(exchange);
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

    @Test
    void requiresResourceWritePermissionForDirectResourceMutation() {
        UUID actor = UUID.randomUUID();
        String path = "/api/resources";
        WebFilterChain chain = mock(WebFilterChain.class);
        AccessControlService accessControl = mock(AccessControlService.class);

        MockServerWebExchange denied = MockServerWebExchange.from(MockServerHttpRequest.post(path).build());
        denied.getAttributes().put(AuthenticatedPrincipal.EXCHANGE_ATTRIBUTE,
            new AuthenticatedPrincipal(actor, UUID.randomUUID(), 0L, java.util.List.of("resource.read")));
        new ResourceAuthorizationWebFilter(accessControl).filter(denied, chain).block();
        assertEquals(403, denied.getResponse().getStatusCode().value());

        MockServerWebExchange allowed = MockServerWebExchange.from(MockServerHttpRequest.post(path).build());
        allowed.getAttributes().put(AuthenticatedPrincipal.EXCHANGE_ATTRIBUTE,
            new AuthenticatedPrincipal(actor, UUID.randomUUID(), 0L, java.util.List.of("resource.write")));
        when(chain.filter(allowed)).thenReturn(Mono.empty());
        new ResourceAuthorizationWebFilter(accessControl).filter(allowed, chain).block();
        verify(chain).filter(allowed);
    }

    @Test
    void appliesResourcePermissionsToDriveRevisionAndTrashRoutes() {
        UUID actor = UUID.randomUUID();
        AccessControlService accessControl = mock(AccessControlService.class);
        WebFilterChain chain = mock(WebFilterChain.class);

        MockServerWebExchange denied = MockServerWebExchange.from(MockServerHttpRequest.post(
            "/api/drive/nodes/" + UUID.randomUUID() + "/trash").build());
        denied.getAttributes().put(AuthenticatedPrincipal.EXCHANGE_ATTRIBUTE,
            new AuthenticatedPrincipal(actor, UUID.randomUUID(), 0L, java.util.List.of("resource.read")));
        new ResourceAuthorizationWebFilter(accessControl).filter(denied, chain).block();
        assertEquals(403, denied.getResponse().getStatusCode().value());

        MockServerWebExchange allowed = MockServerWebExchange.from(MockServerHttpRequest.get(
            "/api/drive/nodes/" + UUID.randomUUID() + "/revisions").build());
        allowed.getAttributes().put(AuthenticatedPrincipal.EXCHANGE_ATTRIBUTE,
            new AuthenticatedPrincipal(actor, UUID.randomUUID(), 0L, java.util.List.of("resource.read")));
        when(accessControl.require(eq(actor), eq(run.ikaros.authentication.api.SecurityVerificationLevel.SVL_0),
            eq(null), any())).thenReturn(Mono.empty());
        when(chain.filter(allowed)).thenReturn(Mono.empty());
        new ResourceAuthorizationWebFilter(accessControl).filter(allowed, chain).block();
        verify(chain).filter(allowed);
    }

    @Test
    void rechecksCurrentResourcePermissionAfterRoleRevocation() {
        UUID actor = UUID.randomUUID();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/resources").build());
        exchange.getAttributes().put(AuthenticatedPrincipal.EXCHANGE_ATTRIBUTE,
            new AuthenticatedPrincipal(actor, UUID.randomUUID(), 0L, java.util.List.of("resource.read")));
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());
        AccessControlService accessControl = (userId, svl, expiresAt, policy) ->
            Mono.error(new run.ikaros.common.ForbiddenException("revoked"));

        new ResourceAuthorizationWebFilter(accessControl).filter(exchange, chain).block();

        assertEquals(403, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void rejectsUnauthenticatedDirectSearchApiCall() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/search?q=book")
            .build());
        new ResourceAuthorizationWebFilter(mock(AccessControlService.class)).filter(exchange,
            mock(WebFilterChain.class)).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void rejectsSearchAfterCurrentResourcePermissionRevocation() {
        UUID actor = UUID.randomUUID();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/search?q=book")
            .build());
        exchange.getAttributes().put(AuthenticatedPrincipal.EXCHANGE_ATTRIBUTE,
            new AuthenticatedPrincipal(actor, UUID.randomUUID(), 0L, java.util.List.of("resource.read")));
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());
        AccessControlService accessControl = (userId, svl, expiresAt, policy) ->
            Mono.error(new run.ikaros.common.ForbiddenException("revoked"));

        new ResourceAuthorizationWebFilter(accessControl).filter(exchange, chain).block();

        assertEquals(403, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void protectsAuditQueryWithAuditReadPermissionAndCurrentRoleCheck() {
        UUID actor = UUID.randomUUID();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/audit-events")
            .build());
        exchange.getAttributes().put(AuthenticatedPrincipal.EXCHANGE_ATTRIBUTE,
            new AuthenticatedPrincipal(actor, UUID.randomUUID(), 0L, java.util.List.of("system.audit.read")));
        WebFilterChain chain = mock(WebFilterChain.class);
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.require(eq(actor), eq(run.ikaros.authentication.api.SecurityVerificationLevel.SVL_0),
            eq(null), any())).thenReturn(Mono.empty());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        new ResourceAuthorizationWebFilter(accessControl).filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(accessControl).require(eq(actor), eq(run.ikaros.authentication.api.SecurityVerificationLevel.SVL_0),
            eq(null), argThat(policy -> policy.permission() == run.ikaros.authorization.api.PlatformPermission.SYSTEM_AUDIT_READ));
    }
}
