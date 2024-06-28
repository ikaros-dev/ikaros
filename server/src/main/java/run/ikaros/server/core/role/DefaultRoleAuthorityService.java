package run.ikaros.server.core.role;

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

    private Mono<Authority> getAuthorityByAuthorityId(Long authorityId) {
        Assert.isTrue(authorityId >= 0, "authorityId must be >= 0");
        return authorityRepository.findById(authorityId)
            .flatMap(entity -> ReactiveBeanUtils.copyProperties(entity, new Authority()));
    }

    @Override
    public Flux<Authority> addAuthoritiesForRole(Long roleId, Long[] authorityIds) {
        Assert.isTrue(roleId >= 0, "roleId must be greater than or equal 0");
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
    public Mono<Void> deleteAuthoritiesForRole(Long roleId, Long[] authorityIds) {
        Assert.isTrue(roleId >= 0, "roleId must be greater than or equal 0");
        Assert.isTrue(authorityIds.length > 0, "authorityIds must be greater than 0");
        return Flux.fromArray(authorityIds)
            .flatMap(authId -> roleAuthorityRepository.deleteByRoleIdAndAuthorityId(roleId, authId))
            .then();
    }

    @Override
    public Flux<Authority> getAuthoritiesForRole(Long roleId) {
        Assert.isTrue(roleId >= 0, "roleId must be greater than or equal 0");
        return roleAuthorityRepository.findByRoleId(roleId)
            .map(RoleAuthorityEntity::getAuthorityId)
            .flatMap(this::getAuthorityByAuthorityId);
    }
}
