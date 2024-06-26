package run.ikaros.server.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.server.core.user.RoleService;
import run.ikaros.server.core.user.User;
import run.ikaros.server.core.user.UserService;
import run.ikaros.server.store.entity.BaseEntity;
import run.ikaros.server.store.entity.UserEntity;
import run.ikaros.server.store.entity.UserRoleEntity;
import run.ikaros.server.store.repository.UserRoleRepository;

@Slf4j
public class MasterInitializer {

    private final SecurityProperties.Initializer initializer;
    private final UserService userService;
    private final RoleService roleService;
    private final UserRoleRepository userRoleRepository;

    /**
     * default master tomoki init.
     *
     * @param initializer security init properties
     * @param userService user service
     * @param roleService role service
     */
    public MasterInitializer(SecurityProperties.Initializer initializer,
                             UserService userService, RoleService roleService,
                             UserRoleRepository userRoleRepository) {
        this.initializer = initializer;
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
        return roleService.createIfNotExist(SecurityConst.ROLE_MASTER)
            .map(BaseEntity::getId)
            .zipWith(Mono.just(UserEntity.builder()
                    .username(initializer.getMasterUsername())
                    .password(getPassword())
                    .nickname(initializer.getMasterNickname())
                    .enable(true)
                    .build())
                .map(User::new)
                .flatMap(userService::save)
                .map(User::entity)
                .map(BaseEntity::getId))
            .flatMap(tuple2 ->
                userRoleRepository.findByUserIdAndRoleId(tuple2.getT2(), tuple2.getT1())
                    .switchIfEmpty(Mono.just(UserRoleEntity.builder()
                        .userId(tuple2.getT2())
                        .roleId(tuple2.getT1())
                        .build())))
            .flatMap(userRoleRepository::save)
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
