package run.ikaros.server.core.role.listener;


import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AuthorityType;
import run.ikaros.server.core.role.event.RoleCreatedEvent;
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
        if (roleEntity == null) {
            throw new NullPointerException();
        }
        String name = roleEntity.getName();
        return configRoleAuthorities(name, roleEntity);

    }

    private Mono<Void> configRoleAuthorities(String name, RoleEntity roleEntity) {
        if (SecurityConst.ROLE_MASTER.equals(name)) {
            UUID roleId = roleEntity.getId();
            if (roleId == null) {
                throw new IllegalArgumentException("roleId must not null.");
            }
            return addMasterAuthority(roleId);
        }
        return Mono.empty();
    }

    private Mono<Void> addMasterAuthority(UUID roleId) {
        return authorityRepository.findByTypeAndTargetAndAuthority(
                AuthorityType.ALL, SecurityConst.Authorization.Target.ALL,
                SecurityConst.Authorization.Authority.ALL)
            .flatMap(entity -> Mono.justOrEmpty(entity.getId()))
            .map(authorityId -> RoleAuthorityEntity.builder()
                .id(UuidV7Utils.generateUuid())
                .authorityId(authorityId)
                .roleId(roleId)
                .build())
            .flatMap(roleAuthorityEntity -> roleAuthorityRepository.insert(roleAuthorityEntity)
                .doOnSuccess(e ->
                    log.debug("save master role authority record: [{}].", e)))
            .then();
    }


}
