package run.ikaros.appruntime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.appruntime.api.AppClientRegistrationView;
import run.ikaros.appruntime.api.AppClientType;
import run.ikaros.appruntime.api.AppDataPolicy;
import run.ikaros.appruntime.api.AppInstallationView;
import run.ikaros.appruntime.api.AppLifecycleState;
import run.ikaros.appruntime.api.InstallAppRequest;
import run.ikaros.appruntime.api.RegisterAppClientRequest;
import run.ikaros.authorization.api.PermissionCatalogQuery;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.integration.api.EventAppendRequest;
import run.ikaros.integration.api.EventReference;
import run.ikaros.operations.api.AuditService;

/** App Runtime Foundation 的状态机和 Owner Boundary 单元测试。 */
class DefaultAppRuntimeServiceTest {
    private AppRuntimeStore store;
    private PermissionCatalogQuery permissionCatalog;
    private DurableEventPublisher events;
    private AuditService audit;
    private TransactionalOperator transaction;
    private DefaultAppRuntimeService service;

    @BeforeEach
    void setUp() {
        store = mock(AppRuntimeStore.class);
        permissionCatalog = mock(PermissionCatalogQuery.class);
        events = mock(DurableEventPublisher.class);
        audit = mock(AuditService.class);
        transaction = mock(TransactionalOperator.class);

        when(events.append(any(EventAppendRequest.class))).thenAnswer(invocation -> {
            EventAppendRequest request = invocation.getArgument(0);
            return Mono.just(new EventReference(UUID.randomUUID(), request.eventType(), request.schemaVersion()));
        });
        when(audit.record(nullable(UUID.class), anyString(), anyString(), nullable(UUID.class), anyString()))
            .thenReturn(Mono.empty());
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(permissionCatalog.isRegistered(anyString())).thenReturn(Mono.just(true));

        service = new DefaultAppRuntimeService(store, permissionCatalog, events, audit, transaction);
    }

    @Test
    void installsAppWithoutEnablingIt() {
        UUID actor = UUID.randomUUID();
        InstallAppRequest request = request();
        AppInstallationView installed = installation(AppLifecycleState.INSTALLED, 0);

        when(store.installation(request.appId())).thenReturn(Mono.empty());
        when(store.upsertDefinition(any(), any())).thenReturn(Mono.empty());
        when(store.replaceScopes(anyString(), any(), any())).thenReturn(Mono.empty());
        when(store.install(any(), any())).thenReturn(Mono.just(installed));

        StepVerifier.create(service.install(actor, request))
            .expectNext(installed)
            .verifyComplete();

        verify(store).replaceScopes(request.appId(), request.scopes(), org.mockito.ArgumentMatchers.any(Instant.class));
        verify(events).append(org.mockito.ArgumentMatchers.argThat(
            event -> "app-runtime.app.installed".equals(event.eventType())
                && "app-runtime".equals(event.producerSubsystem())
        ));
    }

    @Test
    void enablesOnlyInstalledOrDisabledApp() {
        UUID actor = UUID.randomUUID();
        AppInstallationView disabled = installation(AppLifecycleState.DISABLED, 3);
        AppInstallationView enabled = installation(AppLifecycleState.ENABLED, 4);

        when(store.installation("run.ikaros.anime")).thenReturn(Mono.just(disabled));
        when(store.updateLifecycle(
            anyString(),
            org.mockito.ArgumentMatchers.anyLong(),
            any(),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(Mono.just(enabled));

        StepVerifier.create(service.enable(actor, "run.ikaros.anime"))
            .expectNext(enabled)
            .verifyComplete();

        verify(events).append(org.mockito.ArgumentMatchers.argThat(
            event -> "app-runtime.app.enabled".equals(event.eventType())
        ));
    }

    @Test
    void uninstallRequiresDisableFirst() {
        when(store.installation("run.ikaros.anime"))
            .thenReturn(Mono.just(installation(AppLifecycleState.ENABLED, 1)));

        StepVerifier.create(service.uninstall(UUID.randomUUID(), "run.ikaros.anime", AppDataPolicy.KEEP_DATA))
            .expectErrorMatches(error -> error instanceof run.ikaros.common.ConflictException
                && "app.must-disable-first".equals(((run.ikaros.common.ConflictException) error).code()))
            .verify();

        verify(store, never()).updateLifecycle(anyString(), org.mockito.ArgumentMatchers.anyLong(), any(), any(),
            any(), any(), any());
    }

    @Test
    void rejectsDeleteDataUntilErasureHandlerExists() {
        StepVerifier.create(service.uninstall(
                UUID.randomUUID(), "run.ikaros.anime", AppDataPolicy.DELETE_APP_DATA))
            .expectErrorMatches(error -> error instanceof run.ikaros.common.ConflictException
                && "app.data-delete-unsupported".equals(((run.ikaros.common.ConflictException) error).code()))
            .verify();

        verify(store, never()).installation(anyString());
    }

    @Test
    void registersClientAgainstInstalledApp() {
        UUID actor = UUID.randomUUID();
        RegisterAppClientRequest request = new RegisterAppClientRequest(
            "run.ikaros.anime.ios",
            "run.ikaros.anime",
            "Ikaros Anime iOS",
            AppClientType.PUBLIC_NATIVE,
            "Ikaros",
            true,
            List.of("ikaros-anime://oauth/callback")
        );
        AppClientRegistrationView client = new AppClientRegistrationView(
            request.clientId(), request.appId(), request.name(), request.clientType(), request.publisher(),
            request.official(), "ACTIVE", request.redirectUris(), Instant.now(), Instant.now()
        );

        when(store.installation(request.appId()))
            .thenReturn(Mono.just(installation(AppLifecycleState.ENABLED, 2)));
        when(store.insertClient(any(), any())).thenReturn(Mono.just(client));

        StepVerifier.create(service.register(actor, request))
            .expectNext(client)
            .verifyComplete();

        verify(events).append(org.mockito.ArgumentMatchers.argThat(
            event -> "app-runtime.client.registered".equals(event.eventType())
        ));
    }

    @Test
    void rejectsUnregisteredPlatformPermission() {
        when(store.installation("run.ikaros.anime"))
            .thenReturn(Mono.just(installation(AppLifecycleState.INSTALLED, 0)));
        when(permissionCatalog.isRegistered("storage.attachment.read")).thenReturn(Mono.just(false));

        StepVerifier.create(service.replace(
                UUID.randomUUID(),
                "run.ikaros.anime",
                Set.of("storage.attachment.read")))
            .expectError(IllegalArgumentException.class)
            .verify();

        verify(store, never()).replacePermissionGrants(anyString(), any(), any(), any());
    }

    private InstallAppRequest request() {
        return new InstallAppRequest(
            "run.ikaros.anime",
            "Ikaros Anime",
            "Ikaros",
            "1",
            "2.0.0",
            List.of(1),
            "2.0",
            "2.x",
            List.of("anime.library.read", "anime.playback")
        );
    }

    private AppInstallationView installation(AppLifecycleState state, long version) {
        Instant now = Instant.parse("2026-09-19T00:00:00Z");
        return new AppInstallationView(
            "run.ikaros.anime",
            "2.0.0",
            state,
            "2.0",
            "2.x",
            "READY",
            null,
            version,
            now,
            state == AppLifecycleState.ENABLED ? now : null,
            state == AppLifecycleState.DISABLED ? now : null,
            now
        );
    }
}
