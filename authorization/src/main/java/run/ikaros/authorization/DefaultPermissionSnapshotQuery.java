package run.ikaros.authorization;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.authorization.api.PermissionSnapshot;
import run.ikaros.authorization.api.PermissionSnapshotQuery;

/** 从角色绑定实时计算 Authentication 签发 Token 所需的权限快照。 */
@Service
public class DefaultPermissionSnapshotQuery implements PermissionSnapshotQuery {
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public DefaultPermissionSnapshotQuery(UserRoleRepository userRoleRepository,
                                          RolePermissionRepository rolePermissionRepository) {
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    public Mono<PermissionSnapshot> permissionsFor(UUID subjectId) {
        return userRoleRepository.findAllByUserId(subjectId)
            .flatMap(binding -> rolePermissionRepository.findAllByRoleId(binding.roleId()))
            .map(RolePermissionEntity::permissionKey)
            .distinct()
            .sort()
            .collectList()
            .map(keys -> new PermissionSnapshot(subjectId, keys));
    }
}
