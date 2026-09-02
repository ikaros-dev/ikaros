package run.ikaros.identity;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;

/**
 * 默认访问控制服务，禁止高安全验证等级绕过 RBAC 或过期会话的限制。
 */
@Service
public class DefaultAccessControlService implements AccessControlService {
    private final PlatformUserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository permissionRepository;
    private final SecuritySessionRepository sessionRepository;

    /**
     * 创建访问控制服务。
     *
     * @param userRepository 用户仓储
     * @param userRoleRepository 用户角色绑定仓储
     * @param permissionRepository 角色权限绑定仓储
     * @param sessionRepository 安全会话仓储
     */
    public DefaultAccessControlService(PlatformUserRepository userRepository, UserRoleRepository userRoleRepository,
                                       RolePermissionRepository permissionRepository,
                                       SecuritySessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.permissionRepository = permissionRepository;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public Mono<Void> require(UUID userId, UUID sessionId, SecurityPolicy policy) {
        Instant now = Instant.now();
        Mono<Boolean> userIsActive = userRepository.findById(userId)
            .map(user -> user.status() == UserStatus.ACTIVE)
            .defaultIfEmpty(false);
        Mono<Boolean> permitted = userRoleRepository.findAllByUserId(userId)
            .flatMap(binding -> permissionRepository.findByRoleIdAndPermissionKey(binding.roleId(), policy.permission().key()))
            .hasElements();
        Mono<Boolean> sessionSatisfiesPolicy = sessionRepository.findById(sessionId)
            .map(session -> session.userId().equals(userId) && session.revokedAt() == null && session.expiresAt().isAfter(now)
                && session.currentSvl() >= policy.minimumSvl().value()
                && (!policy.requireFreshVerification() || (session.verifiedAt() != null
                    && session.verificationExpiresAt() != null && session.verificationExpiresAt().isAfter(now))))
            .defaultIfEmpty(false);
        return Mono.zip(userIsActive, permitted, sessionSatisfiesPolicy)
            .flatMap(result -> result.getT1() && result.getT2() && result.getT3()
                ? Mono.<Void>empty()
                : Mono.error(new ConflictException("当前身份、权限或安全验证等级不满足操作要求")));
    }
}
