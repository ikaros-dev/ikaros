package run.ikaros.authorization;

import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.authorization.api.InitialRoleAssigner;
import run.ikaros.common.NotFoundException;

/** 为新建用户幂等分配系统初始化角色。 */
@Service
public class DefaultInitialRoleAssigner implements InitialRoleAssigner {
    private final PlatformRoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public DefaultInitialRoleAssigner(PlatformRoleRepository roleRepository, UserRoleRepository userRoleRepository) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public Mono<Void> assignInitialRole(UUID subjectId, String roleCode) {
        return roleRepository.findByCode(roleCode)
            .switchIfEmpty(Mono.error(new NotFoundException("初始角色不存在")))
            .flatMap(role -> userRoleRepository.findByUserIdAndRoleId(subjectId, role.id())
                .hasElement()
                .flatMap(exists -> exists ? Mono.empty() : userRoleRepository.save(new UserRoleEntity(
                    null, subjectId, role.id(), Instant.now(), null)).then())
                .onErrorMap(DataIntegrityViolationException.class,
                    error -> new NotFoundException("用户不存在，无法分配初始角色")))
            .then();
    }
}
