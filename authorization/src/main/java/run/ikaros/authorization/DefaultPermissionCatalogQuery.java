package run.ikaros.authorization;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.authorization.api.PermissionCatalogQuery;

/** 基于 Authorization-owned Permission Registry 的只读查询实现。 */
@Service
public class DefaultPermissionCatalogQuery implements PermissionCatalogQuery {
    private final DatabaseClient databaseClient;

    public DefaultPermissionCatalogQuery(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Boolean> isRegistered(String permissionKey) {
        if (permissionKey == null || permissionKey.isBlank()) {
            return Mono.just(false);
        }
        return databaseClient.sql("""
                SELECT EXISTS (
                    SELECT 1
                    FROM permission_registry
                    WHERE permission_key = :permissionKey
                      AND deprecated = FALSE
                ) AS registered
                """)
            .bind("permissionKey", permissionKey.trim())
            .map((row, metadata) -> Boolean.TRUE.equals(row.get("registered", Boolean.class)))
            .one()
            .defaultIfEmpty(false);
    }
}
