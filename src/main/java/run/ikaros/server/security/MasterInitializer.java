package run.ikaros.server.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.user.User;
import run.ikaros.server.core.user.UserService;
import run.ikaros.server.infra.exception.NotFoundException;
import run.ikaros.server.store.entity.UserEntity;

@Slf4j
public class MasterInitializer {

    private final SecurityProperties.Initializer initializer;
    private final UserService userService;

    /**
     * default master tomoki init.
     *
     * @param initializer security init properties
     * @param userService user service
     */
    public MasterInitializer(SecurityProperties.Initializer initializer,
                             UserService userService) {
        this.initializer = initializer;
        this.userService = userService;
    }

    /**
     * init master user after application ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public Mono<Void> initialize() {
        return userService.getUser(initializer.getMasterUsername())
            .onErrorResume(NotFoundException.class, user -> createMaster())
            .then();
    }

    private Mono<User> createMaster() {
        return Mono.just(UserEntity.builder()
                .username(initializer.getMasterUsername())
                .password(getPassword())
                .nickname(initializer.getMasterNickname())
                .build())
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
