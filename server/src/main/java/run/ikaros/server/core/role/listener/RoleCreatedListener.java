package run.ikaros.server.core.role.listener;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.api.store.enums.AuthorityType;
import run.ikaros.server.core.role.event.RoleCreatedEvent;
import run.ikaros.server.store.entity.BaseEntity;
import run.ikaros.server.store.entity.RoleAuthorityEntity;
import run.ikaros.server.store.entity.RoleEntity;
import run.ikaros.server.store.repository.AuthorityRepository;
import run.ikaros.server.store.repository.RoleAuthorityRepository;

@Slf4j
@Component
public class RoleCreatedListener {

    private final AuthorityRepository authorityRepository;
    private final RoleAuthorityRepository roleAuthorityRepository;

    public RoleCreatedListener(AuthorityRepository authorityRepository,
                               RoleAuthorityRepository roleAuthorityRepository) {
        this.authorityRepository = authorityRepository;
        this.roleAuthorityRepository = roleAuthorityRepository;
    }

    /**
     * Add author after role created.
     *
     * @param event role created event
     */
    @EventListener(RoleCreatedEvent.class)
    public Mono<Void> onRoleCreated(RoleCreatedEvent event) {
        log.debug("RoleCreatedEvent: {}", event);
        final RoleEntity roleEntity = event.getRoleEntity();
        String name = roleEntity.getName();
        return configRoleAuthorities(name, roleEntity);

    }

    private Mono<Void> configRoleAuthorities(String name, RoleEntity roleEntity) {
        if (SecurityConst.ROLE_MASTER.equals(name)) {
            return addMasterAuthority(roleEntity.getId());
        }
        return Mono.empty();
    }

    private Mono<Void> addMasterAuthority(Long roleId) {
        return authorityRepository.findByTypeAndTargetAndAuthority(
                AuthorityType.ALL, SecurityConst.Authorization.Target.ALL,
                SecurityConst.Authorization.Authority.ALL)
            .map(BaseEntity::getId)
            .map(authorityId -> RoleAuthorityEntity.builder()
                .authorityId(authorityId)
                .roleId(roleId)
                .build())
            .flatMap(roleAuthorityEntity -> roleAuthorityRepository.save(roleAuthorityEntity)
                .doOnSuccess(e ->
                    log.debug("save master role authority record: [{}].", e)))
            .then();
    }


}
