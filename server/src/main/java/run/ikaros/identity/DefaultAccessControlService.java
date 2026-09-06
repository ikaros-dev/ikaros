package run.ikaros.identity;

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
    private final PlatformUserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository permissionRepository;

    /**
     * 创建访问控制服务。
     *
     * @param userRepository 用户仓储
     * @param userRoleRepository 用户角色绑定仓储
     * @param permissionRepository 角色权限绑定仓储
     */
    public DefaultAccessControlService(PlatformUserRepository userRepository, UserRoleRepository userRoleRepository,
                                       RolePermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public Mono<Void> require(UUID userId, SecurityVerificationLevel currentSvl,
                               Instant verificationExpiresAt, SecurityPolicy policy) {
        Instant now = Instant.now();
        Mono<Boolean> userIsActive = userRepository.findById(userId)
            .map(user -> user.status() == UserStatus.ACTIVE)
            .defaultIfEmpty(false);
        Mono<Boolean> permitted = userRoleRepository.findAllByUserId(userId)
            .flatMap(binding -> permissionRepository.findByRoleIdAndPermissionKey(binding.roleId(), policy.permission().key()))
            .hasElements();
        boolean svlSatisfied = currentSvl != null && currentSvl.value() >= policy.minimumSvl().value();
        boolean freshnessSatisfied = !policy.requireFreshVerification()
            || (verificationExpiresAt != null && verificationExpiresAt.isAfter(now));
        return Mono.zip(userIsActive, permitted)
            .flatMap(result -> result.getT1() && result.getT2() && svlSatisfied && freshnessSatisfied
                ? Mono.<Void>empty()
                : Mono.error(new ForbiddenException("当前身份、权限或安全验证等级不满足操作要求")));
    }
}
