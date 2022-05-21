package cn.liguohao.ikaros.service;


import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.constants.UserConstants;
import cn.liguohao.ikaros.common.exceptions.UserHasExistException;
import cn.liguohao.ikaros.common.exceptions.UserNoLoginException;
import cn.liguohao.ikaros.config.security.UserDetailsAdapter;
import cn.liguohao.ikaros.entity.Role;
import cn.liguohao.ikaros.entity.User;
import cn.liguohao.ikaros.entity.UserRole;
import cn.liguohao.ikaros.entity.enums.RoleName;
import cn.liguohao.ikaros.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

        Assert.isNotBlank(username, password);

        User existUser = userRepository.findByUsername(username);
        if (existUser != null) {
            throw new UserHasExistException("register user fail, has exist user: username="
                + username);
        }

        // 新增用户
        User userEntity =
            userRepository.save(
                new User()
                    .setEmail("user@liguohao.cn")
                    .setPassword(passwordEncoder.encode(password))
                    .setUsername(username)
                    .setEnable(true)
                    .setIntroduce("no set introduce.")
                    .setNickname("tomoki")
                    .setNonLocked(true)
                    .setSite("http://liguohao.cn")
                    .setTelephone("00000000000"));

        /*
         * 给用户分配角色，新注册用户默认角色是 普通用户
         * @see cn.liguohao.ikaros.uaa.entity.enums.RoleName
         */
        if (roleName == null) {
            roleName = RoleName.COMMON.name();
        }
        Role roleEntity =
            roleService.save(new Role()
                .setName(roleName));

        // 保存 用户角色关系
        userRoleService.save(new UserRole()
            .setUserId(userEntity.getId())
            .setRoleId(roleEntity.getId()));

    }

    public User findByUsername(String username) {
        Assert.isNotBlank(username);
        return userRepository.findByUsername(username);
    }

    /**
     * 注册管理员用户，应该只在第一次启动应用时注册一次
     *
     * @see cn.liguohao.ikaros.init.AdminUserInitAppRunner#run(ApplicationArguments)
     */
    public void initAdminUserOnlyOnce() {
        try {
            registerUserByUsernameAndPassword(UserConstants.DEFAULT_ADMIN_USERNAME,
                UserConstants.DEFAULT_ADMIN_PASSWORD, RoleName.ADMIN.name());
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
    public Set<Role> findRoleByUid(Long id) {
        // 根据用户ID，查找用户角色关系集合
        List<UserRole> userRoles = userRoleService.findByUserId(id);

        // 根据角色ID，查询角色名称
        Set<Role> roles = new HashSet<>();
        for (UserRole userRole : userRoles) {
            Optional<Role> roleOptional = roleService.findById(userRole.getRoleId());
            roleOptional.ifPresent(roles::add);

        }

        return roles;
    }

    /**
     * @return 当前用户登陆后的用户信息
     */
    public User getCurrentLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            UserDetailsAdapter userDetailsAdapter
                = (UserDetailsAdapter) authentication.getPrincipal();
            return userDetailsAdapter.getUser();
        }

        return null;
    }

    /**
     * @return 当前用户登陆的用户ID
     */
    public Long getCurrentLoginUserUid() {
        User currentLoginUser = getCurrentLoginUser();
        if (currentLoginUser == null) {
            throw new UserNoLoginException("please login.");
        }
        return currentLoginUser.getId();
    }
}
