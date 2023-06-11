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
import run.ikaros.server.store.entity.UserEntity;

@Slf4j
public class MasterInitializer {

    private final SecurityProperties.Initializer initializer;
    private final UserService userService;
    private final RoleService roleService;

    /**
     * default master tomoki init.
     *
     * @param initializer security init properties
     * @param userService user service
     * @param roleService role service
     */
    public MasterInitializer(SecurityProperties.Initializer initializer,
                             UserService userService, RoleService roleService) {
        this.initializer = initializer;
        this.userService = userService;
        this.roleService = roleService;
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
            .onErrorResume(UsernameNotFoundException.class, user -> createMaster())
            .then();
    }

    private Mono<User> createMaster() {
        log.debug("Create init user form username={} and role={}",
            initializer.getMasterUsername(), SecurityConst.ROLE_MASTER);
        return roleService.createIfNotExist(SecurityConst.ROLE_MASTER)
            .flatMap(roleEntity -> Mono.just(roleEntity.getId()))
            .flatMap(roleId -> Mono.just(UserEntity.builder()
                .username(initializer.getMasterUsername())
                .password(getPassword())
                .roleId(roleId)
                .nickname(initializer.getMasterNickname())
                .enable(true)
                .build()))
            .map(User::new)
            .flatMap(userService::save);
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
