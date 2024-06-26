package run.ikaros.server.core.user.listener;


import java.lang.reflect.Field;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.api.store.enums.AuthorityType;
import run.ikaros.server.core.user.event.RoleCreatedEvent;
import run.ikaros.server.store.entity.AuthorityEntity;
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
        return initAuthorities()
            .thenEmpty(configRoleAuthorities(name, roleEntity));

    }

    private Mono<Void> configRoleAuthorities(String name, RoleEntity roleEntity) {
        if (SecurityConst.ROLE_MASTER.equals(name)) {
            return addMasterAuthority(roleEntity.getId());
        }
        return Mono.empty();
    }

    private Mono<Void> initAuthorities() {
        Class<SecurityConst.Authorization.Target> targetClass =
            SecurityConst.Authorization.Target.class;
        return Flux.fromArray(targetClass.getFields())
            .flatMap(this::saveAuthoritiesWithTarget)
            .then();
    }

    private Mono<Void> saveAuthoritiesWithTarget(Field tarField) {
        String tarFieldName = tarField.getName();
        AuthorityType authorityType = AuthorityType.OTHERS;
        if (tarFieldName.equalsIgnoreCase("ALL")) {
            authorityType = AuthorityType.ALL;
        }

        if (tarFieldName.startsWith("API") && !tarFieldName.startsWith("APIS")) {
            authorityType = AuthorityType.API;
        }
        if (tarFieldName.startsWith("APIS")) {
            authorityType = AuthorityType.APIS;
        }
        if (tarFieldName.startsWith("MENU")) {
            authorityType = AuthorityType.MENU;
        }
        if (tarFieldName.startsWith("URL")) {
            authorityType = AuthorityType.URL;
        }

        final AuthorityType type = authorityType;
        Class<SecurityConst.Authorization.Authority> authorityClass =
            SecurityConst.Authorization.Authority.class;
        return Flux.fromArray(authorityClass.getFields())
            .map(authField -> AuthorityEntity.builder()
                .allow(true)
                .type(type)
                .authority(String.valueOf(ReflectionUtils.getField(authField, null)))
                .target(String.valueOf(ReflectionUtils.getField(tarField, null)))
                .build())
            .flatMap(authorityEntity -> authorityRepository.save(authorityEntity)
                .doOnSuccess(e -> log.debug("Init new authority record: [{}].", e)))
            .then();
    }

    private Mono<Void> addMasterAuthority(Long roleId) {
        return authorityRepository.findByTypeAndTargetAndAuthority(
                AuthorityType.ALL, SecurityConst.Authorization.Target.ALL,
                SecurityConst.Authorization.Authority.ALL)
            .filter(AuthorityEntity::getAllow)
            .map(BaseEntity::getId)
            .map(authorityId -> RoleAuthorityEntity.builder()
                .authorityId(authorityId)
                .roleId(roleId)
                .build())
            .flatMap(roleAuthorityEntity -> roleAuthorityRepository.save(roleAuthorityEntity)
                .doOnSuccess(e ->
                    log.debug("create master role authority record: [{}].", e)))
            .then();
    }


}
