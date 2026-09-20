package run.ikaros.authorization.api;

import reactor.core.publisher.Mono;

/** 向其他 Platform 模块公开的 Permission Registry 只读能力。 */
public interface PermissionCatalogQuery {
    Mono<Boolean> isRegistered(String permissionKey);
}
