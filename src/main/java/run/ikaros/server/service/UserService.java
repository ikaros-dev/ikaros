package run.ikaros.server.service;


import javax.annotation.Nonnull;

import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.StringUtils;
import run.ikaros.server.constants.RegexConst;
import run.ikaros.server.constants.UserConst;
import run.ikaros.server.utils.BeanUtils;
import run.ikaros.server.utils.JwtUtils;
import run.ikaros.server.exceptions.JwtTokenValidateFailException;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.exceptions.UserHasExistException;
import run.ikaros.server.exceptions.UserLoginFailException;
import run.ikaros.server.model.dto.AuthUserDTO;
import run.ikaros.server.model.dto.UserDTO;
import run.ikaros.server.entity.UserEntity;
import run.ikaros.server.core.repository.UserRepository;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @author li-guohao
 */
@Service
public class UserService extends AbstractCrudService<UserEntity, Long> implements
    run.ikaros.server.core.service.UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * @param userRepository  用户表操作Repo
     * @param passwordEncoder 指定的密码加密实例
     */
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        super(userRepository);
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 通过用户名密码进行注册, 角色是 普通用户。
     *
     * @param username 用户名
     * @param password 密码
     * @return 用户信息
     */
    @Nonnull
    @Override
    public UserEntity registerUserByUsernameAndPassword(
        @Nonnull String username, @Nonnull String password) {
        AssertUtils.notNull(username, "'username' must not be null");
        AssertUtils.notNull(password, "'password' must not be null");

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
                return existUserEntity.hiddenSecretField();
            }
        }

        // 新增用户
        return userRepository.saveAndFlush(
            new UserEntity()
                .setPassword(passwordEncoder.encode(password))
                .setUsername(username)
                .setEnable(true)
                .setIntroduce("no set introduce.")
                .setNickname("nickname")
                .setNonLocked(true)
                .setSite("http://example.org")
                .setTelephone("00000000000")
                .setAvatar(
                    "https://example.org/avator.jpeg")
        );
    }


    public UserEntity findByUsername(@Nonnull String username) {
        AssertUtils.notBlank(username, "'username' must not be blank");
        return userRepository.findByUsername(username);
    }

    public UserEntity findUserById(Long id) throws RecordNotFoundException {
        AssertUtils.isPositive(id, "'id' must be gt 0");
        Optional<UserEntity> userEntityOptional = userRepository.findByIdAndStatus(id, true);
        if (userEntityOptional.isEmpty()) {
            throw new RecordNotFoundException("user not found, id=" + id);
        }
        return userEntityOptional.get();
    }

    @Override
    public void deleteUserById(Long id) {
        AssertUtils.isPositive(id, "'id' must be gt 0");
        UserEntity userEntity = getById(id);
        if (userEntity == null) {
            return;
        }
        userEntity.setStatus(false);
        userRepository.saveAndFlush(userEntity);
    }

    @Override
    public UserEntity updateUserInfo(@Nonnull UserEntity userEntity)
        throws RecordNotFoundException {
        AssertUtils.notNull(userEntity, "'userEntity' must not be null.");
        Long id = userEntity.getId();
        AssertUtils.notNull(id, "user id must not be null");
        UserEntity existUserEntity = findUserById(userEntity.getId());
        BeanUtils.copyProperties(userEntity, existUserEntity);
        existUserEntity = userRepository.saveAndFlush(existUserEntity);
        return existUserEntity;
    }

    @Override
    public UserEntity getUserOnlyOne() {
        Optional<UserEntity> userEntityOptional = listAll().stream().findFirst();
        return userEntityOptional.orElse(null);
    }

    @Nonnull
    public AuthUserDTO login(@Nonnull AuthUserDTO authUserDTO) throws RecordNotFoundException {
        AssertUtils.notNull(authUserDTO, "authUser");
        final String username = authUserDTO.getUsername();
        final String email = authUserDTO.getEmail();
        final String password = authUserDTO.getPassword();
        AssertUtils.isTrue(!(StringUtils.isBlank(username) && StringUtils.isBlank(email)),
            "username or email");
        AssertUtils.notBlank(password, "password");

        UserEntity userEntity = null;
        if (StringUtils.isNotBlank(username)) {
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


        final String token =
            JwtUtils.generateTokenBySubjectAndRoles(
                StringUtils.isNotBlank(username) ? username : email,
                userId, authUserDTO.getRememberMe());
        Authentication authentication = JwtUtils.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        authUserDTO.setId(userId);
        authUserDTO.setToken(token);
        authUserDTO.setPassword("**hidden password**");
        return authUserDTO;
    }

    @Nonnull
    public UserDTO getUserInfoByToken(@Nonnull String token) {
        AssertUtils.notBlank(token, "token");
        if (!JwtUtils.validateToken(token)) {
            throw new JwtTokenValidateFailException("validate fail for token: " + token);
        }

        Authentication authentication = JwtUtils.getAuthentication(token);
        Object principal = authentication.getPrincipal();
        if (principal instanceof String usernameOrEmail) {
            UserEntity userEntity;
            if (usernameOrEmail.matches(RegexConst.EMAIL)) {
                userEntity = userRepository.findByEmail(usernameOrEmail);
            } else {
                userEntity = userRepository.findByUsername(usernameOrEmail);
            }
            if (userEntity != null) {
                userEntity.hiddenSecretField();
            }
            UserDTO userDTO = new UserDTO(userEntity);
            // 填充角色
            userDTO.getRoles().add(UserConst.DEFAULT_ROLE);
            return userDTO;
        } else {
            throw new JwtTokenValidateFailException(
                "validate fail for token principal: " + principal);
        }
    }
}
