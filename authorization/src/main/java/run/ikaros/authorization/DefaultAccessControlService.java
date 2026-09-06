package run.ikaros.authorization;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ForbiddenException;
import run.ikaros.authentication.api.SecurityVerificationLevel;

/**
 * 默认访问控制服务，禁止高安全验证等级绕过 RBAC 或过期验证保证的限制。
 */
@Service
public class DefaultAccessControlService implements AccessControlService {
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository permissionRepository;

    /**
     * 创建访问控制服务。
     *
     * @param userRoleRepository 用户角色绑定仓储
     * @param permissionRepository 角色权限绑定仓储
     */
    public DefaultAccessControlService(UserRoleRepository userRoleRepository,
                                       RolePermissionRepository permissionRepository) {
        this.userRoleRepository = userRoleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public Mono<Void> require(UUID userId, SecurityVerificationLevel currentSvl,
                               Instant verificationExpiresAt, SecurityPolicy policy) {
        Instant now = Instant.now();
        Mono<Boolean> permitted = userRoleRepository.findAllByUserId(userId)
            .flatMap(binding -> permissionRepository.findByRoleIdAndPermissionKey(binding.roleId(), policy.permission().key()))
            .hasElements();
        boolean svlSatisfied = currentSvl != null && currentSvl.value() >= policy.minimumSvl().value();
        boolean freshnessSatisfied = !policy.requireFreshVerification()
            || (verificationExpiresAt != null && verificationExpiresAt.isAfter(now));
        return permitted
            .flatMap(isPermitted -> isPermitted && svlSatisfied && freshnessSatisfied
                ? Mono.<Void>empty()
                : Mono.error(new ForbiddenException("当前身份、权限或安全验证等级不满足操作要求")));
    }
}
