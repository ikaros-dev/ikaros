package run.ikaros.server.core.user.role;

import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import java.util.UUID;
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

    private Mono<Role> findRoleByRoleId(UUID roleId) {
        Assert.notNull(roleId, "'roleId' must not null.");
        return roleRepository.findById(roleId)
            .flatMap(roleEntity -> copyProperties(roleEntity, new Role()));
    }

    @Override
    public Mono<UserRoleEntity> saveEntity(UserRoleEntity entity) {
        Assert.notNull(entity, "roleEntity must not be null");
        Assert.notNull(entity.getUserId(), "'userId' must not null.");
        Assert.notNull(entity.getRoleId(), "'roleId' must not null.");
        return userRoleRepository.findByUserIdAndRoleId(entity.getUserId(), entity.getRoleId())
            .map(e -> e.setUserId(entity.getUserId()).setRoleId(entity.getRoleId()))
            .switchIfEmpty(Mono.just(entity))
            .flatMap(userRoleRepository::save);
    }

    @Override
    public Flux<Role> addUserRoles(UUID userId, UUID[] roleIds) {
        Assert.notNull(userId, "'userId' must not null.");
        Assert.notNull(roleIds, "'roleIds' must not null.");
        Assert.isTrue(roleIds.length > 0, "roleIds must be greater than zero");
        return Flux.fromArray(roleIds)
            .map(roleId -> UserRoleEntity.builder()
                .userId(userId).roleId(roleId)
                .build())
            .flatMap(this::saveEntity)
            .map(UserRoleEntity::getRoleId)
            .flatMap(this::findRoleByRoleId);
    }

    @Override
    public Mono<Void> deleteUserRoles(UUID userId, UUID[] roleIds) {
        Assert.notNull(userId, "'userId' must not null.");
        Assert.notNull(roleIds, "'roleIds' must not null.");
        Assert.isTrue(roleIds.length > 0, "roleIds must be greater than zero");
        return Flux.fromArray(roleIds)
            .flatMap(roleId -> userRoleRepository.deleteByUserIdAndRoleId(userId, roleId))
            .then();
    }

    @Override
    public Flux<Role> getRolesForUser(UUID userId) {
        Assert.notNull(userId, "'userId' must not null.");
        return userRoleRepository.findByUserId(userId)
            .map(UserRoleEntity::getRoleId)
            .flatMap(this::findRoleByRoleId);
    }
}
