package run.ikaros.authorization;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.authorization.api.RoleMembershipQuery;

/** 查询用户当前绑定的角色编码。 */
@Service
public class DefaultRoleMembershipQuery implements RoleMembershipQuery {
    private final UserRoleRepository userRoleRepository;
    private final PlatformRoleRepository roleRepository;

    public DefaultRoleMembershipQuery(UserRoleRepository userRoleRepository, PlatformRoleRepository roleRepository) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public Mono<java.util.List<String>> roleCodesFor(UUID subjectId) {
        return userRoleRepository.findAllByUserId(subjectId)
            .flatMap(binding -> roleRepository.findById(binding.roleId()))
            .map(PlatformRoleEntity::code)
            .distinct()
            .sort()
            .collectList();
    }
}
