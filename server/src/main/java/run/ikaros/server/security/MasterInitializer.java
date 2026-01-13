package run.ikaros.server.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.api.core.role.Role;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.core.role.RoleService;
import run.ikaros.server.core.user.User;
import run.ikaros.server.core.user.UserService;
import run.ikaros.server.store.entity.BaseEntity;
import run.ikaros.server.store.entity.UserEntity;
import run.ikaros.server.store.entity.UserRoleEntity;
import run.ikaros.server.store.repository.UserRoleRepository;

@Slf4j
@Component
@ConditionalOnProperty(name = "ikaros.security.initializer.disabled",
    havingValue = "false",
    matchIfMissing = true)
public class MasterInitializer {

    private final SecurityProperties securityProperties;
    private final SecurityProperties.Initializer initializer;
    private final UserService userService;
    private final RoleService roleService;
    private final UserRoleRepository userRoleRepository;

    /**
     * default master tomoki init.
     */
    public MasterInitializer(SecurityProperties securityProperties,
                             UserService userService, RoleService roleService,
                             UserRoleRepository userRoleRepository) {
        this.securityProperties = securityProperties;
        this.initializer = this.securityProperties.getInitializer();
        this.userService = userService;
        this.roleService = roleService;
        this.userRoleRepository = userRoleRepository;
    }

    /**
     * init master user after application ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public Mono<Void> initialize() {
        if (initializer.isDisabled()) {
            log.warn("Skip init master user when ikaros.security.initializer.disabled=true");
            return Mono.empty();
        }
        return userService.getUserByUsername(initializer.getMasterUsername())
            .then()
            .onErrorResume(UsernameNotFoundException.class, user ->
                userService.count()
                    .filter(count -> count == 0)
                    .flatMap(count -> createMaster()));
    }

    private Mono<Void> createMaster() {
        log.debug("Create init user form username={} and role={}",
            initializer.getMasterUsername(), SecurityConst.ROLE_MASTER);
        return roleService.save(Role.builder()
                .id(UuidV7Utils.generateUuid())
                .name(SecurityConst.ROLE_MASTER)
                .description("Default admin role, unable delete")
                .build())
            .map(Role::getId)
            .zipWith(Mono.just(UserEntity.builder()
                    .username(initializer.getMasterUsername())
                    .password(getPassword())
                    .nickname(initializer.getMasterNickname())
                    .enable(true)
                    .build())
                .map(userEntity -> {
                    if (userEntity.getId() == null) {
                        userEntity.setId(UuidV7Utils.generateUuid());
                    }
                    return userEntity;
                })
                .map(User::new)
                .flatMap(userService::insert)
                .map(User::entity)
                .map(BaseEntity::getId))
            .flatMap(tuple2 ->
                userRoleRepository.findByUserIdAndRoleId(tuple2.getT2(), tuple2.getT1())
                    .switchIfEmpty(userRoleRepository.insert(UserRoleEntity.builder()
                        .id(UuidV7Utils.generateUuid())
                        .userId(tuple2.getT2())
                        .roleId(tuple2.getT1())
                        .build())))
            .flatMap(userRoleRepository::update)
            .then();
    }


    private String getPassword() {
        var password = this.initializer.getMasterPassword();
        if (!StringUtils.hasText(password)) {
            // generate password
            password = RandomStringUtils.randomAlphanumeric(16);
            log.info("=== Generated random password: {} for super master: {} ===",
                password, this.initializer.getMasterUsername());
        }
        return password;
    }
}
