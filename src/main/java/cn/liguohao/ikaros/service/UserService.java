package cn.liguohao.ikaros.service;


import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.Strings;
import cn.liguohao.ikaros.common.constants.RegexConstants;
import cn.liguohao.ikaros.common.constants.SecurityConstants;
import cn.liguohao.ikaros.common.constants.UserConstants;
import cn.liguohao.ikaros.common.kit.BeanKit;
import cn.liguohao.ikaros.common.kit.JwtKit;
import cn.liguohao.ikaros.exceptions.JwtTokenValidateFailException;
import cn.liguohao.ikaros.exceptions.RecordNotFoundException;
import cn.liguohao.ikaros.exceptions.UserHasExistException;
import cn.liguohao.ikaros.exceptions.UserLoginFailException;
import cn.liguohao.ikaros.init.MasterUserInitAppRunner;
import cn.liguohao.ikaros.model.dto.AuthUserDTO;
import cn.liguohao.ikaros.model.dto.RoleDTO;
import cn.liguohao.ikaros.model.dto.UserDTO;
import cn.liguohao.ikaros.model.entity.RoleEntity;
import cn.liguohao.ikaros.model.entity.UserEntity;
import cn.liguohao.ikaros.model.entity.UserRoleEntity;
import cn.liguohao.ikaros.model.enums.Role;
import cn.liguohao.ikaros.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @author li-guohao
 */
@Service
public class UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;

    /**
     * @param userRepository  用户表操作Repo
     * @param passwordEncoder 指定的密码加密实例
     * @param roleService     角色服务
     * @param userRoleService 用户角色关系服务
     */
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       RoleService roleService,
                       UserRoleService userRoleService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;
        this.userRoleService = userRoleService;
    }

    /**
     * 通过用户名密码进行注册, 角色是 普通用户。
     *
     * @param username 用户名
     * @param password 密码
     */
    public void registerUserByUsernameAndPassword(
        String username, String password) {
        registerUserByUsernameAndPassword(username, password, null);
    }

    /**
     * 通过用户名密码进行注册
     *
     * @param username 用户名
     * @param password 密码
     * @param roleName 角色
     */
    public synchronized void registerUserByUsernameAndPassword(
        String username, String password, String roleName) {

        Assert.notNull(username, "'username' must not be null");
        Assert.notNull(password, "'password' must not be null");

        UserEntity existUserEntity = userRepository.findByUsername(username);
        if (existUserEntity != null) {
            if (existUserEntity.getStatus()) {
                throw new UserHasExistException("register user fail, has exist user: username="
                    + username);
            } else {
                // 当用户名已经存在，但是用户被逻辑删除了，则更新状态重新启动，并直接返回
                existUserEntity.setStatus(true);
                userRepository.saveAndFlush(existUserEntity);
                LOGGER.info("enable exist user, username={}", username);
                return;
            }
        }

        // 新增用户
        UserEntity userEntity =
            userRepository.save(
                new UserEntity()
                    .setEmail(UserConstants.DEFAULT_MASTER_EMAIL)
                    .setPassword(passwordEncoder.encode(password))
                    .setUsername(username)
                    .setEnable(true)
                    .setIntroduce("no set introduce.")
                    .setNickname("nickname")
                    .setNonLocked(true)
                    .setSite("http://blog.liguohao.cn")
                    .setTelephone("00000000000")
                    .setAvatar(
                        "https://openimg.liguohao.cn/avator.jpeg")
            );

        // 给用户分配角色，新注册用户默认角色是 访客
        if (roleName == null) {
            roleName = Role.VISITOR.name();
        } else {
            if (!Role.contains(roleName)) {
                throw new IllegalArgumentException(
                    "illegal role=" + roleName + "; you can select on in: " + Role.getRoleNames());
            }
        }
        RoleEntity roleEntity =
            roleService.save(new RoleEntity()
                .setName(roleName));

        // 保存 用户角色关系
        userRoleService.save(new UserRoleEntity()
            .setUserId(userEntity.getId())
            .setRoleId(roleEntity.getId()));

    }

    public UserEntity findByUsername(String username) {
        Assert.notBlank(username, "'username' must not be blank");
        return userRepository.findByUsername(username);
    }

    /**
     * 注册管理员用户，应该只在第一次启动应用时注册一次
     *
     * @see MasterUserInitAppRunner#run(ApplicationArguments)
     */
    public void initMasterUserOnlyOnce() {
        try {
            registerUserByUsernameAndPassword(UserConstants.DEFAULT_MASTER_USERNAME,
                UserConstants.DEFAULT_MASTER_PASSWORD, Role.MASTER.name());
        } catch (UserHasExistException userHasExistException) {
            // 说明：这里捕获这个异常不进行处理，因为数据库管理员用户只需要注册一次就行了
        }
    }

    /**
     * 查找用户所拥有的角色
     *
     * @param id 用户ID
     * @return 角色集合
     */
    public Set<RoleEntity> findRoleByUid(Long id) {
        // 根据用户ID，查找用户角色关系集合
        List<UserRoleEntity> userRoleEntities = userRoleService.findByUserId(id);

        // 根据角色ID，查询角色名称
        Set<RoleEntity> roleEntities = new HashSet<>();
        for (UserRoleEntity userRoleEntity : userRoleEntities) {
            Optional<RoleEntity> roleOptional = roleService.findById(userRoleEntity.getRoleId());
            roleOptional.ifPresent(roleEntities::add);

        }

        return roleEntities;
    }

    /**
     * @return 当前用户登陆的用户ID
     */
    public static Long getCurrentLoginUserUid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String token = (String) authentication.getCredentials();
            Integer userId =
                (Integer) JwtKit.getTokenHeaderValue(token, SecurityConstants.HEADER_UID);
            return Long.valueOf(userId);
        }
        return UserConstants.UID_WHEN_NO_AUTH;
    }

    public UserEntity findUserById(Long id) throws RecordNotFoundException {
        Assert.isPositive(id, "'id' must be gt 0");
        Optional<UserEntity> userEntityOptional = userRepository.findByIdAndStatus(id, true);
        if (userEntityOptional.isEmpty()) {
            throw new RecordNotFoundException("user not found, id=" + id);
        }
        return userEntityOptional.get();
    }

    public void deleteUserById(Long id) throws RecordNotFoundException {
        Assert.isPositive(id, "'id' must be gt 0");
        UserEntity userEntity = findUserById(id);
        userEntity.setStatus(false);
        userRepository.saveAndFlush(userEntity);
    }

    public UserEntity updateUserInfo(UserEntity userEntity) throws RecordNotFoundException {
        Assert.notNull(userEntity, "'userEntity' must not be null.");
        Long id = userEntity.getId();
        Assert.notNull(id, "user id must not be null");
        UserEntity existUserEntity = findUserById(userEntity.getId());
        BeanKit.copyProperties(userEntity, existUserEntity);
        existUserEntity = userRepository.saveAndFlush(existUserEntity);
        return existUserEntity;
    }

    public AuthUserDTO login(AuthUserDTO authUserDTO) throws RecordNotFoundException {
        Assert.notNull(authUserDTO, "'authUser' must not be null");
        final String username = authUserDTO.getUsername();
        final String email = authUserDTO.getEmail();
        final String password = authUserDTO.getPassword();
        Assert.isTrue(!(Strings.isBlank(username) && Strings.isBlank(email)),
            "'username' or 'email' must not be null");
        Assert.hasText(password, "'password' must not be null");

        UserEntity userEntity = null;
        if (Strings.isNotBlank(username)) {
            // username login
            userEntity = userRepository.findByUsername(username);
        } else {
            // email login
            userEntity = userRepository.findByEmail(email);
        }
        if (userEntity == null) {
            throw new RecordNotFoundException("user not found for username=" + username);
        }
        final Long userId = userEntity.getId();

        if (!passwordEncoder.matches(password, userEntity.getPassword())) {
            throw new UserLoginFailException("login fail for username=" + username);
        }

        List<String> roles = userRoleService.findByUserId(userId).stream().flatMap(
            (Function<UserRoleEntity, Stream<String>>) userRoleEntity -> {
                final Long roleId = userRoleEntity.getRoleId();
                Optional<RoleEntity> roleEntityOptional = roleService.findById(roleId);
                return roleEntityOptional.isEmpty() ? null :
                    Stream.of(
                        UserConstants.SECURITY_ROLE_PREFIX + roleEntityOptional.get().getName());
            }).collect(Collectors.toList());

        final String token =
            JwtKit.generateTokenBySubjectAndRoles(Strings.isNotBlank(username) ? username : email,
                userId, roles, authUserDTO.getRememberMe());
        Authentication authentication = JwtKit.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        authUserDTO.setId(userId);
        authUserDTO.setToken(token);
        authUserDTO.setPassword("**hidden password**");
        return authUserDTO;
    }

    public UserDTO getUserInfoByToken(String token) {
        Assert.hasText(token, "'token' must not be null");
        if (!JwtKit.validateToken(token)) {
            throw new JwtTokenValidateFailException("validate fail for token: " + token);
        }

        Authentication authentication = JwtKit.getAuthentication(token);
        Object principal = authentication.getPrincipal();
        if (principal instanceof String usernameOrEmail) {
            UserEntity userEntity;
            if (usernameOrEmail.matches(RegexConstants.EMAIL)) {
                userEntity = userRepository.findByEmail(usernameOrEmail);
            } else {
                userEntity = userRepository.findByUsername(usernameOrEmail);
            }
            userEntity.setPassword(SecurityConstants.HIDDEN_STR);
            UserDTO userDTO = new UserDTO(userEntity);

            // 填充角色
            userRoleService.findByUserId(userEntity.getId()).forEach(userRoleEntity -> {
                Optional<RoleEntity> roleEntityOptional =
                    roleService.findById(userRoleEntity.getRoleId());
                roleEntityOptional.ifPresent(
                    roleEntity -> userDTO.getRoles().add(new RoleDTO(roleEntity)));
            });
            return userDTO;
        } else {
            throw new JwtTokenValidateFailException(
                "validate fail for token principal: " + principal);
        }
    }
}
