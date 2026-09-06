package run.ikaros.authorization.api;

import java.util.UUID;
import reactor.core.publisher.Mono;

/** Authorization 向其他模块公开的权限快照查询能力。 */
public interface PermissionSnapshotQuery {
    Mono<PermissionSnapshot> permissionsFor(UUID subjectId);
}
