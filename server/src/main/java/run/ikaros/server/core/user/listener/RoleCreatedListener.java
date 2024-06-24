package run.ikaros.server.core.user.listener;


import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.api.store.enums.AuthorityType;
import run.ikaros.server.core.user.event.RoleCreatedEvent;
import run.ikaros.server.infra.utils.JsonUtils;
import run.ikaros.server.store.entity.AuthorityEntity;
import run.ikaros.server.store.entity.RoleEntity;
import run.ikaros.server.store.repository.AuthorityRepository;

@Slf4j
@Component
public class RoleCreatedListener {

    private final AuthorityRepository authorityRepository;

    public RoleCreatedListener(AuthorityRepository authorityRepository) {
        this.authorityRepository = authorityRepository;
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
        if (SecurityConst.ROLE_MASTER.equals(name)) {
            return addMasterAuthority(roleEntity.getId());
        }

        if (SecurityConst.ROLE_FRIEND.equals(name)) {
            return addFriendAuthority(roleEntity.getId());
        }

        return Mono.empty();
    }

    private Mono<Void> addMasterAuthority(Long roleId) {
        AuthorityEntity authorityEntity = AuthorityEntity.builder().roleId(roleId)
            .type(AuthorityType.ALL).allow(true).target("*").authority("*").build();
        return authorityRepository.save(authorityEntity)
            .doOnSuccess(e ->
                log.debug("create new master authority: {}", authorityEntity))
            .then();
    }

    private Mono<Void> addFriendAuthority(Long roleId) {
        AuthorityEntity authorityEntity = AuthorityEntity.builder().roleId(roleId)
            .type(AuthorityType.ALL).allow(true).target("*").authority(
                JsonUtils.obj2Json(List.of("GET"))
            ).build();
        return authorityRepository.save(authorityEntity)
            .doOnSuccess(e ->
                log.debug("create new friend authority: {}", authorityEntity))
            .then();
    }


}
