package run.ikaros.server.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.api.store.entity.Role;
import run.ikaros.api.store.entity.User;
import run.ikaros.api.store.entity.UserRole;
import run.ikaros.server.store.mapper.RoleMapper;
import run.ikaros.server.store.mapper.UserMapper;
import run.ikaros.server.store.mapper.UserRoleMapper;

@Slf4j
@Component
@ConditionalOnProperty(name = "ikaros.security.initializer.disabled",
    havingValue = "false",
    matchIfMissing = true)
public class MasterInitializer {

    private final SecurityProperties securityProperties;
    private final SecurityProperties.Initializer initializer;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    /**
     * default master tomoki init.
     */
    public MasterInitializer(SecurityProperties securityProperties, UserMapper userMapper,
                             RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.securityProperties = securityProperties;
        this.initializer = securityProperties.getInitializer();
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }


    /**
     * init master user after application ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        if (initializer.isDisabled()) {
            log.warn("Skip init master user when ikaros.security.initializer.disabled=true");
            return;
        }

        boolean exists = userMapper.exists(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, initializer.getMasterUsername()));

        if (exists && !(userMapper.exists(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, initializer.getMasterUsername())
            .eq(User::getEnable, true)
            .eq(User::getDeleteStatus, false)))) {
            LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<User>();
            updateWrapper.set(User::getEnable, true);
            updateWrapper.set(User::getDeleteStatus, false);
            userMapper.update(updateWrapper);
            log.info("Update and enable current user for {}.", initializer.getMasterUsername());
            return;
        }

        // To create master user
        LocalDateTime now = LocalDateTime.now();
        Role role = new Role();
        role.setName(SecurityConst.ROLE_MASTER);
        role.setDescription("Default admin role, unable delete");
        role.setDeleteStatus(false);
        role.setCreateTime(now);
        role.setUpdateTime(now);
        role.setCreateUid(-1L);
        role.setUpdateUid(-1L);
        roleMapper.insertOrUpdate(role);
        log.info("Insert or update master role: [{}].", role);

        User user = new User();
        user.setUsername(initializer.getMasterUsername());
        user.setPassword(getPassword());
        user.setNickname(initializer.getMasterNickname());
        user.setEnable(true);
        user.setDeleteStatus(false);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        user.setCreateUid(-1L);
        user.setUpdateUid(-1L);
        userMapper.insertOrUpdate(user);
        log.info("Insert or update master user: [{}].", user);

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRoleMapper.insertOrUpdate(userRole);
        log.info("Insert or update master userRole: [{}].", userRole);

        log.info("Create init user success form username={} and role={}",
            user.getUsername(), role.getName());
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
