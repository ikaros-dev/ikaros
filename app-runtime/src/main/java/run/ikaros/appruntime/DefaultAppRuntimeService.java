package run.ikaros.appruntime;

import java.net.URI;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.appruntime.api.AppClientRegistrationView;
import run.ikaros.appruntime.api.AppClientRegistry;
import run.ikaros.appruntime.api.AppDataPolicy;
import run.ikaros.appruntime.api.AppDefinitionView;
import run.ikaros.appruntime.api.AppInstallationView;
import run.ikaros.appruntime.api.AppLifecycleCommand;
import run.ikaros.appruntime.api.AppLifecycleState;
import run.ikaros.appruntime.api.AppPlatformPermissionGrantService;
import run.ikaros.appruntime.api.AppRegistryQuery;
import run.ikaros.appruntime.api.InstallAppRequest;
import run.ikaros.appruntime.api.RegisterAppClientRequest;
import run.ikaros.authorization.api.PermissionCatalogQuery;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.integration.api.EventAppendRequest;
import run.ikaros.operations.api.AuditService;

/** App Registry、Lifecycle、Client Registration 与 Platform Permission Grant 的首个持久化实现。 */
@Service
public class DefaultAppRuntimeService implements
    AppRegistryQuery,
    AppLifecycleCommand,
    AppClientRegistry,
    AppPlatformPermissionGrantService {

    private static final Pattern APP_ID = Pattern.compile("[a-z0-9][a-z0-9.-]{2,127}");
    private static final Pattern KEY = Pattern.compile("[a-z][a-z0-9]*(?:[._-][a-z0-9]+)+");

    private final AppRuntimeStore store;
    private final PermissionCatalogQuery permissionCatalog;
    private final DurableEventPublisher events;
    private final AuditService audit;
    private final TransactionalOperator transaction;

    public DefaultAppRuntimeService(
        AppRuntimeStore store,
        PermissionCatalogQuery permissionCatalog,
        DurableEventPublisher events,
        AuditService audit,
        TransactionalOperator transaction
    ) {
        this.store = store;
        this.permissionCatalog = permissionCatalog;
        this.events = events;
        this.audit = audit;
        this.transaction = transaction;
    }

    @Override
    public Mono<AppInstallationView> install(UUID actorId, InstallAppRequest request) {
        validateInstall(request);
        Mono<AppInstallationView> operation = store.installation(request.appId())
            .flatMap(existing -> existing.lifecycleState() == AppLifecycleState.UNINSTALLED
                ? persistInstall(actorId, request)
                : Mono.error(new ConflictException("app.already-installed", "App 已安装")))
            .switchIfEmpty(Mono.defer(() -> persistInstall(actorId, request)));
        return transactional(operation);
    }

    private Mono<AppInstallationView> persistInstall(UUID actorId, InstallAppRequest request) {
        Instant now = Instant.now();
        return store.upsertDefinition(request, now)
            .then(store.replaceScopes(request.appId(), request.scopes(), now))
            .then(store.install(request, now))
            .flatMap(saved -> emit(
                    "app-runtime.app.installed",
                    object(
                        stringField("app_id", request.appId()),
                        stringField("package_version", request.packageVersion())
                    )
                )
                .then(audit(actorId, "app-runtime.app.install", request.appId()))
                .thenReturn(saved));
    }

    @Override
    public Mono<AppInstallationView> enable(UUID actorId, String appId) {
        Mono<AppInstallationView> operation = requiredInstallation(appId).flatMap(current -> {
            if (current.lifecycleState() == AppLifecycleState.ENABLED) {
                return Mono.just(current);
            }
            if (!EnumSet.of(AppLifecycleState.INSTALLED, AppLifecycleState.DISABLED)
                .contains(current.lifecycleState())) {
                return invalidState(appId, current.lifecycleState(), "enable");
            }
            Instant now = Instant.now();
            return store.updateLifecycle(
                    current.appId(), current.version(), AppLifecycleState.ENABLED, null, now, null, now
                )
                .flatMap(saved -> emit(
                        "app-runtime.app.enabled",
                        object(
                            stringField("app_id", saved.appId()),
                            stringField("package_version", saved.packageVersion())
                        )
                    )
                    .then(audit(actorId, "app-runtime.app.enable", saved.appId()))
                    .thenReturn(saved));
        });
        return transactional(operation);
    }

    @Override
    public Mono<AppInstallationView> disable(UUID actorId, String appId, String reasonCode) {
        Mono<AppInstallationView> operation = requiredInstallation(appId).flatMap(current -> {
            if (current.lifecycleState() == AppLifecycleState.DISABLED) {
                return Mono.just(current);
            }
            if (current.lifecycleState() != AppLifecycleState.ENABLED) {
                return invalidState(appId, current.lifecycleState(), "disable");
            }
            Instant now = Instant.now();
            return store.updateLifecycle(
                    current.appId(), current.version(), AppLifecycleState.DISABLED, null, null, now, now
                )
                .flatMap(saved -> emit(
                        "app-runtime.app.disabled",
                        object(
                            stringField("app_id", saved.appId()),
                            nullableStringField("reason_code", reasonCode)
                        )
                    )
                    .then(audit(actorId, "app-runtime.app.disable", saved.appId()))
                    .thenReturn(saved));
        });
        return transactional(operation);
    }

    @Override
    public Mono<AppInstallationView> uninstall(UUID actorId, String appId, AppDataPolicy dataPolicy) {
        if (dataPolicy == null) {
            return Mono.error(new IllegalArgumentException("必须明确 App-owned Data 处理策略"));
        }
        if (dataPolicy == AppDataPolicy.DELETE_APP_DATA) {
            return Mono.error(new ConflictException(
                "app.data-delete-unsupported",
                "Foundation 阶段尚未注册 App-owned Data Erasure Handler"
            ));
        }
        Mono<AppInstallationView> operation = requiredInstallation(appId).flatMap(current -> {
            if (current.lifecycleState() == AppLifecycleState.UNINSTALLED) {
                return Mono.just(current);
            }
            if (current.lifecycleState() == AppLifecycleState.ENABLED) {
                return Mono.error(new ConflictException("app.must-disable-first", "卸载前必须先停用 App"));
            }
            if (!EnumSet.of(
                    AppLifecycleState.INSTALLED,
                    AppLifecycleState.DISABLED,
                    AppLifecycleState.FAILED,
                    AppLifecycleState.INCOMPATIBLE
                ).contains(current.lifecycleState())) {
                return invalidState(appId, current.lifecycleState(), "uninstall");
            }
            Instant now = Instant.now();
            return store.updateLifecycle(
                    current.appId(),
                    current.version(),
                    AppLifecycleState.UNINSTALLED,
                    null,
                    null,
                    current.disabledAt(),
                    now
                )
                .flatMap(saved -> emit(
                        "app-runtime.app.uninstalled",
                        object(
                            stringField("app_id", saved.appId()),
                            stringField("package_version", saved.packageVersion()),
                            stringField("data_policy", dataPolicy.name())
                        )
                    )
                    .then(audit(actorId, "app-runtime.app.uninstall", saved.appId()))
                    .thenReturn(saved));
        });
        return transactional(operation);
    }

    @Override
    public Mono<AppDefinitionView> definition(String appId) {
        return store.definition(normalizeId(appId))
            .switchIfEmpty(Mono.error(new NotFoundException("app.not-installed", "App 不存在")));
    }

    @Override
    public Mono<AppInstallationView> installation(String appId) {
        return requiredInstallation(appId);
    }

    @Override
    public Flux<AppInstallationView> installations() {
        return store.installations();
    }

    @Override
    public Flux<String> scopes(String appId) {
        String normalized = normalizeId(appId);
        return store.installation(normalized)
            .switchIfEmpty(Mono.error(new NotFoundException("app.not-installed", "App 不存在")))
            .thenMany(store.scopes(normalized));
    }

    @Override
    public Mono<AppClientRegistrationView> register(UUID actorId, RegisterAppClientRequest request) {
        validateClient(request);
        Mono<AppClientRegistrationView> operation = requiredInstallation(request.appId())
            .filter(installation -> installation.lifecycleState() != AppLifecycleState.UNINSTALLED)
            .switchIfEmpty(Mono.error(new NotFoundException("app.not-installed", "目标 App 未安装")))
            .then(store.insertClient(request, Instant.now()))
            .onErrorMap(
                DuplicateKeyException.class,
                error -> new ConflictException("app.client-invalid", "Client ID 已注册")
            )
            .flatMap(saved -> emit(
                    "app-runtime.client.registered",
                    object(
                        stringField("client_id", saved.clientId()),
                        stringField("app_id", saved.appId()),
                        stringField("client_type", saved.clientType().name()),
                        booleanField("official", saved.official())
                    )
                )
                .then(audit(actorId, "app-runtime.client.register", saved.appId()))
                .thenReturn(saved));
        return transactional(operation);
    }

    @Override
    public Mono<AppClientRegistrationView> disable(UUID actorId, String clientId) {
        Mono<AppClientRegistrationView> operation = get(clientId).flatMap(current -> {
            if ("DISABLED".equals(current.status())) {
                return Mono.just(current);
            }
            if (!"ACTIVE".equals(current.status())) {
                return Mono.error(new ConflictException("app.client-invalid", "Client 当前状态不可停用"));
            }
            return store.updateClientStatus(current.clientId(), "DISABLED", Instant.now())
                .flatMap(saved -> emit(
                        "app-runtime.client.disabled",
                        object(
                            stringField("client_id", saved.clientId()),
                            stringField("app_id", saved.appId())
                        )
                    )
                    .then(audit(actorId, "app-runtime.client.disable", saved.appId()))
                    .thenReturn(saved));
        });
        return transactional(operation);
    }

    @Override
    public Mono<AppClientRegistrationView> get(String clientId) {
        String normalized = requiredText(clientId, "client_id");
        return store.client(normalized)
            .switchIfEmpty(Mono.error(new NotFoundException("app.client-invalid", "Client 不存在")));
    }

    @Override
    public Flux<AppClientRegistrationView> listByApp(String appId) {
        return store.clientsByApp(normalizeId(appId));
    }

    @Override
    public Mono<Void> replace(UUID actorId, String appId, Set<String> permissionKeys) {
        String normalized = normalizeId(appId);
        Set<String> keys = permissionKeys == null ? Set.of() : Set.copyOf(permissionKeys);
        Mono<Void> operation = requiredInstallation(normalized)
            .filter(installation -> installation.lifecycleState() != AppLifecycleState.UNINSTALLED)
            .switchIfEmpty(Mono.error(new NotFoundException("app.not-installed", "App 未安装")))
            .then(validatePermissions(keys))
            .then(Mono.defer(() -> store.replacePermissionGrants(normalized, keys, actorId, Instant.now())))
            .then(Mono.defer(() -> emit(
                "app-runtime.app.platform-permissions-replaced",
                object(
                    stringField("app_id", normalized),
                    stringArrayField("permission_keys", keys)
                )
            )))
            .then(Mono.defer(() -> audit(actorId, "app-runtime.permission.replace", normalized)));
        return transactional(operation);
    }

    @Override
    public Flux<String> grantedPermissions(String appId) {
        return store.grantedPermissions(normalizeId(appId));
    }

    private Mono<Void> validatePermissions(Set<String> keys) {
        return Flux.fromIterable(keys)
            .concatMap(key -> permissionCatalog.isRegistered(key)
                .flatMap(registered -> registered
                    ? Mono.empty()
                    : Mono.error(new IllegalArgumentException("未注册的 Platform Permission: " + key))))
            .then();
    }

    private Mono<AppInstallationView> requiredInstallation(String appId) {
        String normalized = normalizeId(appId);
        return store.installation(normalized)
            .switchIfEmpty(Mono.error(new NotFoundException("app.not-installed", "App 未安装")));
    }

    private <T> Mono<T> invalidState(String appId, AppLifecycleState state, String action) {
        return Mono.error(new ConflictException(
            "app.invalid-state",
            "App " + appId + " 当前状态 " + state + " 不允许执行 " + action
        ));
    }

    private Mono<Void> emit(String eventType, String payload) {
        return events.append(new EventAppendRequest(
            eventType, 1, "app-runtime", "server_app", null, payload
        )).then();
    }

    private Mono<Void> audit(UUID actorId, String action, String appId) {
        return audit.record(
            actorId,
            action,
            "SERVER_APP",
            null,
            object(stringField("app_id", appId))
        );
    }

    private <T> Mono<T> transactional(Mono<T> operation) {
        return transaction == null ? operation : operation.as(transaction::transactional);
    }

    private void validateInstall(InstallAppRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Install request 不能为空");
        }
        requireCanonicalAppId(request.appId());
        requiredText(request.name(), "name");
        requiredText(request.publisher(), "publisher");
        requiredText(request.manifestVersion(), "manifest_version");
        requiredText(request.packageVersion(), "package_version");
        requiredText(request.platformApiMin(), "platform_api_min");
        if (request.supportedApiMajors().isEmpty()
            || request.supportedApiMajors().stream().anyMatch(version -> version == null || version < 1)) {
            throw new IllegalArgumentException("supported_api_majors 必须包含正整数");
        }
        request.scopes().forEach(this::validateKey);
    }

    private void validateClient(RegisterAppClientRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Client request 不能为空");
        }
        requiredText(request.clientId(), "client_id");
        requireCanonicalAppId(request.appId());
        requiredText(request.name(), "name");
        requiredText(request.publisher(), "publisher");
        if (request.clientType() == null) {
            throw new IllegalArgumentException("client_type 不能为空");
        }
        request.redirectUris().forEach(this::validateRedirectUri);
    }

    private void validateRedirectUri(String value) {
        String text = requiredText(value, "redirect_uri");
        URI uri;
        try {
            uri = URI.create(text);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("redirect_uri 不合法", error);
        }
        if (!uri.isAbsolute() || uri.getScheme() == null || uri.getFragment() != null) {
            throw new IllegalArgumentException("redirect_uri 必须为无 fragment 的绝对 URI");
        }
    }

    private void requireCanonicalAppId(String value) {
        String raw = requiredText(value, "app_id");
        String canonical = normalizeId(raw);
        if (!raw.equals(canonical)) {
            throw new IllegalArgumentException("app_id 必须使用小写 canonical reverse-DNS 标识");
        }
    }

    private String normalizeId(String value) {
        String appId = requiredText(value, "app_id").toLowerCase();
        if (!APP_ID.matcher(appId).matches()
            || appId.contains("..")
            || appId.endsWith(".")
            || appId.endsWith("-")) {
            throw new IllegalArgumentException("app_id 必须使用稳定 reverse-DNS 风格标识");
        }
        return appId;
    }

    private void validateKey(String value) {
        String key = requiredText(value, "key");
        if (!KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Scope / Permission Key 格式不合法: " + key);
        }
    }

    private String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
        return value.trim();
    }

    private String object(String... fields) {
        return "{" + String.join(",", fields) + "}";
    }

    private String stringField(String name, String value) {
        return quote(name) + ":" + quote(value);
    }

    private String nullableStringField(String name, String value) {
        return quote(name) + ":" + (value == null || value.isBlank() ? "null" : quote(value.trim()));
    }

    private String booleanField(String name, boolean value) {
        return quote(name) + ":" + value;
    }

    private String stringArrayField(String name, Set<String> values) {
        String body = String.join(",", values.stream().sorted().map(this::quote).toList());
        return quote(name) + ":[" + body + "]";
    }

    private String quote(String value) {
        return Character.toString(34) + json(value) + Character.toString(34);
    }

    private String json(String value) {
        return value == null
            ? ""
            : value.replace(Character.toString(92), Character.toString(92) + Character.toString(92))
                .replace(Character.toString(34), Character.toString(92) + Character.toString(34));
    }
}
