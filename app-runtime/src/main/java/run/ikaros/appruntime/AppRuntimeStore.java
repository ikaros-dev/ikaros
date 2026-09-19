package run.ikaros.appruntime;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.appruntime.api.AppClientRegistrationView;
import run.ikaros.appruntime.api.AppClientType;
import run.ikaros.appruntime.api.AppDefinitionView;
import run.ikaros.appruntime.api.AppInstallationView;
import run.ikaros.appruntime.api.AppLifecycleState;
import run.ikaros.appruntime.api.InstallAppRequest;
import run.ikaros.appruntime.api.RegisterAppClientRequest;

/** App Runtime 对 app_runtime owner schema 的唯一持久化入口。 */
@Repository
class AppRuntimeStore {
    private final DatabaseClient databaseClient;

    AppRuntimeStore(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    Mono<Void> upsertDefinition(InstallAppRequest request, Instant now) {
        Integer[] apiMajors = request.supportedApiMajors().toArray(Integer[]::new);
        return databaseClient.sql("""
                INSERT INTO app_runtime.app_definition (
                    app_id, name, publisher, manifest_version, supported_api_majors, created_at, updated_at
                )
                VALUES (:appId, :name, :publisher, :manifestVersion, :apiMajors, :now, :now)
                ON CONFLICT (app_id) DO UPDATE SET
                    name = EXCLUDED.name,
                    publisher = EXCLUDED.publisher,
                    manifest_version = EXCLUDED.manifest_version,
                    supported_api_majors = EXCLUDED.supported_api_majors,
                    updated_at = EXCLUDED.updated_at
                """)
            .bind("appId", request.appId())
            .bind("name", request.name())
            .bind("publisher", request.publisher())
            .bind("manifestVersion", request.manifestVersion())
            .bind("apiMajors", apiMajors)
            .bind("now", now)
            .fetch()
            .rowsUpdated()
            .then();
    }

    Mono<Void> replaceScopes(String appId, List<String> scopes, Instant now) {
        Mono<Void> delete = databaseClient.sql("""
                DELETE FROM app_runtime.app_scope_definition WHERE app_id = :appId
                """)
            .bind("appId", appId)
            .fetch()
            .rowsUpdated()
            .then();
        Flux<Void> inserts = Flux.fromIterable(scopes.stream().distinct().sorted().toList())
            .concatMap(scope -> databaseClient.sql("""
                    INSERT INTO app_runtime.app_scope_definition (
                        app_id, scope_key, description, created_at, updated_at
                    )
                    VALUES (:appId, :scopeKey, :description, :now, :now)
                    """)
                .bind("appId", appId)
                .bind("scopeKey", scope)
                .bind("description", scope)
                .bind("now", now)
                .fetch()
                .rowsUpdated()
                .then());
        return delete.thenMany(inserts).then();
    }

    Mono<AppInstallationView> install(InstallAppRequest request, Instant now) {
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql("""
                INSERT INTO app_runtime.app_installation (
                    app_id, package_version, lifecycle_state, platform_api_min, platform_api_max,
                    configuration_state, failure_code, version, installed_at, enabled_at, disabled_at, updated_at
                )
                VALUES (
                    :appId, :packageVersion, 'INSTALLED', :platformApiMin, :platformApiMax,
                    'READY', NULL, 0, :now, NULL, NULL, :now
                )
                ON CONFLICT (app_id) DO UPDATE SET
                    package_version = EXCLUDED.package_version,
                    lifecycle_state = 'INSTALLED',
                    platform_api_min = EXCLUDED.platform_api_min,
                    platform_api_max = EXCLUDED.platform_api_max,
                    configuration_state = 'READY',
                    failure_code = NULL,
                    version = app_runtime.app_installation.version + 1,
                    installed_at = EXCLUDED.installed_at,
                    enabled_at = NULL,
                    disabled_at = NULL,
                    updated_at = EXCLUDED.updated_at
                """)
            .bind("appId", request.appId())
            .bind("packageVersion", request.packageVersion())
            .bind("platformApiMin", request.platformApiMin())
            .bind("now", now);
        spec = bindNullable(spec, "platformApiMax", request.platformApiMax(), String.class);
        return spec.fetch().rowsUpdated().then(installation(request.appId()));
    }

    Mono<AppInstallationView> installation(String appId) {
        return databaseClient.sql("""
                SELECT app_id, package_version, lifecycle_state, platform_api_min, platform_api_max,
                       configuration_state, failure_code, version, installed_at, enabled_at, disabled_at, updated_at
                FROM app_runtime.app_installation
                WHERE app_id = :appId
                """)
            .bind("appId", appId)
            .map((row, metadata) -> installationView(row))
            .one();
    }

    Flux<AppInstallationView> installations() {
        return databaseClient.sql("""
                SELECT app_id, package_version, lifecycle_state, platform_api_min, platform_api_max,
                       configuration_state, failure_code, version, installed_at, enabled_at, disabled_at, updated_at
                FROM app_runtime.app_installation
                ORDER BY app_id
                """)
            .map((row, metadata) -> installationView(row))
            .all();
    }

    Mono<AppInstallationView> updateLifecycle(
        String appId,
        long expectedVersion,
        AppLifecycleState state,
        String failureCode,
        Instant enabledAt,
        Instant disabledAt,
        Instant now
    ) {
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql("""
                UPDATE app_runtime.app_installation
                SET lifecycle_state = :state,
                    failure_code = :failureCode,
                    enabled_at = :enabledAt,
                    disabled_at = :disabledAt,
                    updated_at = :now,
                    version = version + 1
                WHERE app_id = :appId
                  AND version = :expectedVersion
                """)
            .bind("state", state.name())
            .bind("now", now)
            .bind("appId", appId)
            .bind("expectedVersion", expectedVersion);
        spec = bindNullable(spec, "failureCode", failureCode, String.class);
        spec = bindNullable(spec, "enabledAt", enabledAt, Instant.class);
        spec = bindNullable(spec, "disabledAt", disabledAt, Instant.class);
        return spec.fetch().rowsUpdated()
            .flatMap(updated -> updated == 1
                ? installation(appId)
                : Mono.error(new IllegalStateException("App lifecycle version conflict: " + appId)));
    }

    Mono<AppDefinitionView> definition(String appId) {
        return databaseClient.sql("""
                SELECT app_id, name, publisher, manifest_version, supported_api_majors
                FROM app_runtime.app_definition
                WHERE app_id = :appId
                """)
            .bind("appId", appId)
            .map((row, metadata) -> new DefinitionRow(
                row.get("app_id", String.class),
                row.get("name", String.class),
                row.get("publisher", String.class),
                row.get("manifest_version", String.class),
                array(row.get("supported_api_majors", Integer[].class))
            ))
            .one()
            .flatMap(definition -> scopes(appId).collectList()
                .map(scopes -> new AppDefinitionView(
                    definition.appId(),
                    definition.name(),
                    definition.publisher(),
                    definition.manifestVersion(),
                    definition.supportedApiMajors(),
                    scopes
                )));
    }

    Flux<String> scopes(String appId) {
        return databaseClient.sql("""
                SELECT scope_key
                FROM app_runtime.app_scope_definition
                WHERE app_id = :appId
                ORDER BY scope_key
                """)
            .bind("appId", appId)
            .map((row, metadata) -> row.get("scope_key", String.class))
            .all();
    }

    Mono<AppClientRegistrationView> insertClient(RegisterAppClientRequest request, Instant now) {
        return databaseClient.sql("""
                INSERT INTO app_runtime.app_client_registration (
                    client_id, app_id, name, client_type, publisher, official, status, created_at, updated_at
                )
                VALUES (
                    :clientId, :appId, :name, :clientType, :publisher, :official, 'ACTIVE', :now, :now
                )
                """)
            .bind("clientId", request.clientId())
            .bind("appId", request.appId())
            .bind("name", request.name())
            .bind("clientType", request.clientType().name())
            .bind("publisher", request.publisher())
            .bind("official", request.official())
            .bind("now", now)
            .fetch()
            .rowsUpdated()
            .then(replaceRedirectUris(request.clientId(), request.redirectUris(), now))
            .then(client(request.clientId()));
    }

    Mono<Void> replaceRedirectUris(String clientId, List<String> redirectUris, Instant now) {
        Mono<Void> delete = databaseClient.sql("""
                DELETE FROM app_runtime.app_client_redirect_uri WHERE client_id = :clientId
                """)
            .bind("clientId", clientId)
            .fetch()
            .rowsUpdated()
            .then();
        Flux<Void> inserts = Flux.fromIterable(redirectUris.stream().distinct().sorted().toList())
            .concatMap(uri -> databaseClient.sql("""
                    INSERT INTO app_runtime.app_client_redirect_uri (client_id, redirect_uri, created_at)
                    VALUES (:clientId, :redirectUri, :now)
                    """)
                .bind("clientId", clientId)
                .bind("redirectUri", uri)
                .bind("now", now)
                .fetch()
                .rowsUpdated()
                .then());
        return delete.thenMany(inserts).then();
    }

    Mono<AppClientRegistrationView> client(String clientId) {
        return databaseClient.sql("""
                SELECT client_id, app_id, name, client_type, publisher, official, status, created_at, updated_at
                FROM app_runtime.app_client_registration
                WHERE client_id = :clientId
                """)
            .bind("clientId", clientId)
            .map((row, metadata) -> new ClientRow(
                row.get("client_id", String.class),
                row.get("app_id", String.class),
                row.get("name", String.class),
                AppClientType.valueOf(row.get("client_type", String.class)),
                row.get("publisher", String.class),
                Boolean.TRUE.equals(row.get("official", Boolean.class)),
                row.get("status", String.class),
                row.get("created_at", Instant.class),
                row.get("updated_at", Instant.class)
            ))
            .one()
            .flatMap(client -> redirectUris(client.clientId()).collectList()
                .map(uris -> client.view(uris)));
    }

    Flux<AppClientRegistrationView> clientsByApp(String appId) {
        return databaseClient.sql("""
                SELECT client_id
                FROM app_runtime.app_client_registration
                WHERE app_id = :appId
                ORDER BY client_id
                """)
            .bind("appId", appId)
            .map((row, metadata) -> row.get("client_id", String.class))
            .all()
            .concatMap(this::client);
    }

    Mono<AppClientRegistrationView> updateClientStatus(String clientId, String status, Instant now) {
        return databaseClient.sql("""
                UPDATE app_runtime.app_client_registration
                SET status = :status, updated_at = :now
                WHERE client_id = :clientId
                """)
            .bind("status", status)
            .bind("now", now)
            .bind("clientId", clientId)
            .fetch()
            .rowsUpdated()
            .then(client(clientId));
    }

    Flux<String> redirectUris(String clientId) {
        return databaseClient.sql("""
                SELECT redirect_uri
                FROM app_runtime.app_client_redirect_uri
                WHERE client_id = :clientId
                ORDER BY redirect_uri
                """)
            .bind("clientId", clientId)
            .map((row, metadata) -> row.get("redirect_uri", String.class))
            .all();
    }

    Mono<Void> replacePermissionGrants(String appId, Set<String> permissionKeys, Instant now) {
        Mono<Void> delete = databaseClient.sql("""
                DELETE FROM app_runtime.app_permission_grant WHERE app_id = :appId
                """)
            .bind("appId", appId)
            .fetch()
            .rowsUpdated()
            .then();
        Flux<Void> inserts = Flux.fromIterable(permissionKeys.stream().sorted().toList())
            .concatMap(key -> databaseClient.sql("""
                    INSERT INTO app_runtime.app_permission_grant (
                        app_id, permission_key, grant_status, granted_by_user_id, granted_at, updated_at
                    )
                    VALUES (:appId, :permissionKey, 'GRANTED', NULL, :now, :now)
                    """)
                .bind("appId", appId)
                .bind("permissionKey", key)
                .bind("now", now)
                .fetch()
                .rowsUpdated()
                .then());
        return delete.thenMany(inserts).then();
    }

    Flux<String> grantedPermissions(String appId) {
        return databaseClient.sql("""
                SELECT permission_key
                FROM app_runtime.app_permission_grant
                WHERE app_id = :appId
                  AND grant_status = 'GRANTED'
                ORDER BY permission_key
                """)
            .bind("appId", appId)
            .map((row, metadata) -> row.get("permission_key", String.class))
            .all();
    }

    private AppInstallationView installationView(io.r2dbc.spi.Row row) {
        return new AppInstallationView(
            row.get("app_id", String.class),
            row.get("package_version", String.class),
            AppLifecycleState.valueOf(row.get("lifecycle_state", String.class)),
            row.get("platform_api_min", String.class),
            row.get("platform_api_max", String.class),
            row.get("configuration_state", String.class),
            row.get("failure_code", String.class),
            value(row.get("version", Long.class)),
            row.get("installed_at", Instant.class),
            row.get("enabled_at", Instant.class),
            row.get("disabled_at", Instant.class),
            row.get("updated_at", Instant.class)
        );
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private List<Integer> array(Integer[] values) {
        return values == null ? List.of() : List.of(values);
    }

    private <T> DatabaseClient.GenericExecuteSpec bindNullable(
        DatabaseClient.GenericExecuteSpec spec,
        String name,
        T value,
        Class<T> type
    ) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }

    private record DefinitionRow(
        String appId,
        String name,
        String publisher,
        String manifestVersion,
        List<Integer> supportedApiMajors
    ) { }

    private record ClientRow(
        String clientId,
        String appId,
        String name,
        AppClientType clientType,
        String publisher,
        boolean official,
        String status,
        Instant createdAt,
        Instant updatedAt
    ) {
        AppClientRegistrationView view(List<String> redirectUris) {
            return new AppClientRegistrationView(
                clientId, appId, name, clientType, publisher, official, status, redirectUris, createdAt, updatedAt
            );
        }
    }
}
