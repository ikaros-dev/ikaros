package run.ikaros.server.core.role;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.authority.Authority;
import run.ikaros.api.infra.utils.ReactiveBeanUtils;
import run.ikaros.server.store.entity.RoleAuthorityEntity;
import run.ikaros.server.store.repository.AuthorityRepository;
import run.ikaros.server.store.repository.RoleAuthorityRepository;

@Slf4j
@Service
public class DefaultRoleAuthorityService implements RoleAuthorityService {
    private final RoleAuthorityRepository roleAuthorityRepository;
    private final AuthorityRepository authorityRepository;

    public DefaultRoleAuthorityService(RoleAuthorityRepository roleAuthorityRepository,
                                       AuthorityRepository authorityRepository) {
        this.roleAuthorityRepository = roleAuthorityRepository;
        this.authorityRepository = authorityRepository;
    }

    private Mono<Authority> getAuthorityByAuthorityId(UUID authorityId) {
        Assert.notNull(authorityId, "'authorityId' must not null.");
        return authorityRepository.findById(authorityId)
            .flatMap(entity -> ReactiveBeanUtils.copyProperties(entity, new Authority()));
    }

    @Override
    public Flux<Authority> addAuthoritiesForRole(UUID roleId, UUID[] authorityIds) {
        Assert.notNull(roleId, "'roleId' must not null.");
        Assert.isTrue(authorityIds.length > 0, "authorityIds must be greater than 0");
        return Flux.fromArray(authorityIds)
            .map(authorityId -> RoleAuthorityEntity.builder()
                .roleId(roleId).authorityId(authorityId)
                .build())
            .flatMap(roleAuthorityRepository::save)
            .map(RoleAuthorityEntity::getAuthorityId)
            .flatMap(this::getAuthorityByAuthorityId);
    }

    @Override
    public Mono<Void> deleteAuthoritiesForRole(UUID roleId, UUID[] authorityIds) {
        Assert.notNull(roleId, "'roleId' must not null.");
        Assert.isTrue(authorityIds.length > 0, "authorityIds must be greater than 0");
        return Flux.fromArray(authorityIds)
            .flatMap(authId -> roleAuthorityRepository.deleteByRoleIdAndAuthorityId(roleId, authId))
            .then();
    }

    @Override
    public Flux<Authority> getAuthoritiesForRole(UUID roleId) {
        Assert.notNull(roleId, "'roleId' must not null.");
        return roleAuthorityRepository.findByRoleId(roleId)
            .map(RoleAuthorityEntity::getAuthorityId)
            .flatMap(this::getAuthorityByAuthorityId);
    }
}
