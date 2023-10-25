package run.ikaros.server.core.user;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static run.ikaros.server.core.user.UserService.addEncodingIdPrefixIfNotExists;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Example;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import run.ikaros.api.infra.exception.IkarosNotFoundException;
import run.ikaros.api.infra.exception.security.PasswordNotMatchingException;
import run.ikaros.server.store.entity.UserEntity;
import run.ikaros.server.store.repository.RoleRepository;
import run.ikaros.server.store.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Construct.
     */
    public UserServiceImpl(UserRepository repository, RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           ApplicationEventPublisher applicationEventPublisher) {
        this.repository = repository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public Mono<User> save(User user) {
        Assert.notNull(user, "'user' must not be null");
        Assert.notNull(user.entity(), "'user entity' must not be null");
        return Mono.just(user)
            .map(User::entity)
            .map(userEntity -> userEntity.setPassword(
                passwordEncoder.encode(userEntity.getPassword())))
            .flatMap(repository::save)
            .map(User::new);
    }

    @Override
    public Mono<Void> deleteAll() {
        return repository.deleteAll();
    }

    @Override
    public Mono<User> getUserByUsername(String username) {
        Assert.hasText(username, "'username' must has text");
        return repository
            .findOne(Example.of(
                UserEntity.builder()
                    .username(username)
                    .build()))
            .switchIfEmpty(Mono.error(() -> new UsernameNotFoundException(
                String.format("Not found for username=%s", username))))
            .map(User::new);
    }

    @Override
    public Mono<Void> updatePassword(@NotBlank String username, @NotBlank String oldRawPassword,
                                     @NotBlank String rawPassword) {
        Assert.hasText(username, "'username' must has text");
        Assert.hasText(oldRawPassword, "'oldRawPassword' must has text");
        Assert.hasText(rawPassword, "'rawPassword' must has text");
        return getUserByUsername(username)
            .map(User::entity)
            .filter(userEntity -> passwordEncoder.matches(oldRawPassword,
                addEncodingIdPrefixIfNotExists(userEntity.getPassword())))
            .switchIfEmpty(Mono.error(
                new PasswordNotMatchingException(
                    "Old password not matching username: " + username)))
            .filter(userEntity -> !StringUtils.hasText(userEntity.getPassword())
                || !passwordEncoder.matches(rawPassword,
                addEncodingIdPrefixIfNotExists(userEntity.getPassword())))
            .map(userEntity -> userEntity.setPassword(passwordEncoder.encode(rawPassword)))
            .flatMap(repository::save)
            .then();
    }

    @Override
    public Mono<User> update(@Nonnull UpdateUserRequest updateUserRequest) {
        Assert.notNull(updateUserRequest, "'updateUserRequest' must not be null.");
        String username = updateUserRequest.getUsername();
        Assert.hasText(username, "'username' must has text.");
        return repository.findByUsernameAndEnableAndDeleteStatus(username, true, false)
            .switchIfEmpty(
                Mono.error(new IkarosNotFoundException("User not found for username=" + username)))
            .flatMap(userEntity -> updateEntity(userEntity, updateUserRequest))
            .map(User::new);
    }

    @Override
    public Mono<Void> bindEmail(@NotBlank String username, @NotBlank String email,
                                @NotBlank String verificationCode) {
        Assert.hasText(username, "'username' must not blank.");
        Assert.hasText(email, "'email' must not blank.");
        Assert.hasText(verificationCode, "'verificationCode' must not blank.");
        // todo bing email
        return null;
    }

    @Override
    public Mono<Void> bindTelephone(@NotBlank String username, @NotBlank String telephone,
                                    @NotBlank String verificationCode) {
        Assert.hasText(username, "'username' must not blank.");
        Assert.hasText(telephone, "'telephone' must not blank.");
        Assert.hasText(verificationCode, "'verificationCode' must not blank.");
        // todo bing telephone
        return null;
    }

    @Override
    public Mono<Void> changeRole(String username, Long roleId) {
        Assert.hasText(username, "'username' must not blank.");
        Assert.isTrue(roleId != null && roleId > 0, "'roleId' must not null and must gt 0.");
        return repository.existsByUsername(username)
            .flatMap(r -> r ? roleRepository.existsById(roleId) :
                Mono.error(new IkarosNotFoundException("User not exists for username=" + username)))
            .flatMap(r ->
                r ? repository.findByUsernameAndEnableAndDeleteStatus(username, true, false)
                    : Mono.error(new IkarosNotFoundException("Role not exists for id=" + roleId)))
            .map(userEntity -> userEntity.setRoleId(roleId))
            .flatMap(repository::save)
            .then();
    }


    @Nonnull
    private Mono<UserEntity> updateEntity(UserEntity userEntity,
                                          UpdateUserRequest updateUserRequest) {
        Assert.notNull(userEntity, "'userEntity' must not null.");
        Assert.notNull(updateUserRequest, "'updateUserRequest' must not null.");
        if (isNotBlank(updateUserRequest.getNickname())) {
            userEntity.setNickname(updateUserRequest.getNickname());
        }
        if (isNotBlank(updateUserRequest.getIntroduce())) {
            userEntity.setIntroduce(updateUserRequest.getIntroduce());
        }
        if (isNotBlank(updateUserRequest.getSite())) {
            userEntity.setSite(updateUserRequest.getSite());
        }
        if (!updateUserRequest.getAvatar().equalsIgnoreCase(userEntity.getAvatar())) {
            String oldAvatar = userEntity.getAvatar();
            userEntity.setAvatar(updateUserRequest.getAvatar());
            UserAvatarUpdateEvent event =
                new UserAvatarUpdateEvent(this, oldAvatar, updateUserRequest.getAvatar(),
                    userEntity.getId(), userEntity.getUsername());
            applicationEventPublisher.publishEvent(event);
        }
        return repository.save(userEntity);
    }
}
