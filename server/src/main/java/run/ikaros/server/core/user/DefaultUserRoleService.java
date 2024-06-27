package run.ikaros.server.core.user;

import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.role.Role;
import run.ikaros.server.store.entity.UserRoleEntity;
import run.ikaros.server.store.repository.RoleRepository;
import run.ikaros.server.store.repository.UserRoleRepository;

@Slf4j
@Service
public class DefaultUserRoleService implements UserRoleService {
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    public DefaultUserRoleService(UserRoleRepository userRoleRepository,
                                  RoleRepository roleRepository) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
    }

    private Mono<Role> findRoleByRoleId(Long roleId) {
        Assert.isTrue(roleId >= 0, "roleId must be greater than or equal 0");
        return roleRepository.findById(roleId)
            .flatMap(roleEntity -> copyProperties(roleEntity, new Role()));
    }

    @Override
    public Flux<Role> addUserRoles(Long userId, Long[] roleIds) {
        Assert.isTrue(userId >= 0, "userId must be greater than zero");
        Assert.isTrue(roleIds.length > 0, "roleIds must be greater than zero");
        return Flux.fromArray(roleIds)
            .map(roleId -> UserRoleEntity.builder()
                .userId(userId).roleId(roleId)
                .build())
            .flatMap(userRoleRepository::save)
            .map(UserRoleEntity::getRoleId)
            .flatMap(this::findRoleByRoleId);
    }

    @Override
    public Mono<Void> deleteUserRoles(Long userId, Long[] roleIds) {
        Assert.isTrue(userId >= 0, "userId must be greater than zero");
        Assert.isTrue(roleIds.length > 0, "roleIds must be greater than zero");
        return Flux.fromArray(roleIds)
            .flatMap(roleId -> userRoleRepository.deleteByUserIdAndRoleId(userId, roleId))
            .then();
    }

    @Override
    public Flux<Role> getRolesForUser(Long userId) {
        Assert.isTrue(userId >= 0, "userId must be greater than zero");
        return userRoleRepository.findByUserId(userId)
            .map(UserRoleEntity::getRoleId)
            .flatMap(this::findRoleByRoleId);
    }
}
