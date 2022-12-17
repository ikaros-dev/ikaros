package run.ikaros.server.service;


import jakarta.annotation.Nonnull;

import run.ikaros.server.exceptions.RuntimeIkarosException;
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
                return existUserEntity;
            }
        }

        // 新增用户
        UserEntity userEntity = new UserEntity();
        userEntity.setPassword(passwordEncoder.encode(password));
        userEntity.setUsername(username);
        userEntity.setEnable(true);
        userEntity.setIntroduce("no set introduce.");
        userEntity.setNickname("no set nickname");
        userEntity.setNonLocked(true);
        userEntity.setSite("");
        userEntity.setTelephone("");
        userEntity.setAvatar("");
        return userRepository.saveAndFlush(userEntity);
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
        if (StringUtils.isNotBlank(userEntity.getPassword())) {
            userEntity.setPassword(passwordEncoder.encode(userEntity.getPassword()));
        }
        BeanUtils.copyProperties(userEntity, existUserEntity);
        existUserEntity = userRepository.saveAndFlush(existUserEntity);
        return existUserEntity;
    }

    @Override
    public UserEntity getUserOnlyOne() {
        Optional<UserEntity> userEntityOptional = listAll().stream().findFirst();
        return userEntityOptional.orElse(null);
    }

    @Override
    public void updatePassword(@Nonnull Long userId, @Nonnull String oldPassword,
                               @Nonnull String newPassword) {
        AssertUtils.notNull(userId, "userId");
        AssertUtils.notBlank(oldPassword, "oldPassword");
        AssertUtils.notBlank(newPassword, "newPassword");
        UserEntity userEntity = getById(userId);
        if (userEntity == null) {
            throw new RecordNotFoundException("not found user with id=" + userId);
        }
        if (!passwordEncoder.matches(oldPassword, userEntity.getPassword())) {
            throw new RuntimeIkarosException("incorrect old password: " + oldPassword);
        }
        userEntity.setPassword(passwordEncoder.encode(newPassword));
        save(userEntity);
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
        authUserDTO.setPassword(null);
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
            UserDTO userDTO = new UserDTO(userEntity);
            userDTO.setPassword(null);
            // 填充角色
            userDTO.getRoles().add(UserConst.DEFAULT_ROLE);
            return userDTO;
        } else {
            throw new JwtTokenValidateFailException(
                "validate fail for token principal: " + principal);
        }
    }
}
