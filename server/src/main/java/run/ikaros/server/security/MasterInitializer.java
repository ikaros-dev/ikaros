package run.ikaros.server.security;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.jspecify.annotations.Nullable;
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
import run.ikaros.server.store.entity.UserEntity;
import run.ikaros.server.store.entity.UserRoleEntity;
import run.ikaros.server.store.repository.UserRoleRepository;

/**
 * 负责初始化系统默认管理员账户.
 */
@Slf4j
@Component
public class MasterInitializer {

    /** 初始账户配置. */
    private final SecurityProperties.Initializer initializer;

    /** 用户服务. */
    private final UserService userService;

    /** 角色服务. */
    private final RoleService roleService;

    /** 用户角色关系仓库. */
    private final UserRoleRepository userRoleRepository;

    /** 本次启动首次创建账户时使用的明文密码. */
    private @Nullable String initialPassword;

    /**
     * default master tomoki init.
     */
    public MasterInitializer(SecurityProperties securityProperties,
                             UserService userService, RoleService roleService,
                             UserRoleRepository userRoleRepository) {
        this.initializer = securityProperties.getInitializer();
        this.userService = userService;
        this.roleService = roleService;
        this.userRoleRepository = userRoleRepository;
    }

    /**
     * 初始化默认管理员账户.
     */
    public Mono<Void> initialize() {
        initialPassword = null;
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
            .flatMap(role -> Mono.justOrEmpty(role.getId()))
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
                .flatMap(entity -> Mono.justOrEmpty(entity.getId())))
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
            password = RandomStringUtils.secure().nextAlphanumeric(16);
            log.info("=== Generated random password: {} for super master: {} ===",
                password, this.initializer.getMasterUsername());
        }
        this.initialPassword = password;
        return password;
    }

    /**
     * 获取本次启动首次创建账户时使用的明文密码.
     *
     * @return 仅当本次启动创建了初始账户时返回密码
     */
    public Optional<String> getInitialPassword() {
        return Optional.ofNullable(initialPassword);
    }
}
