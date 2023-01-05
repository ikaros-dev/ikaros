package run.ikaros.server.core.user;

import org.springframework.data.domain.Example;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.UserEntity;
import run.ikaros.server.store.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
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
    public Mono<User> getUser(String username) {
        Assert.hasText(username, "'username' must has text");
        return repository
            .findOne(Example.of(
                UserEntity.builder()
                    .username(username)
                    .build()))
            .switchIfEmpty(Mono.error(() -> new UserNotFoundException(
                String.format("user not found for username=%s", username))))
            .map(User::new);
    }

    @Override
    public Mono<User> updatePassword(String username, String rawPassword) {
        Assert.hasText(username, "'username' must has text");
        Assert.hasText(rawPassword, "'rawPassword' must has text");
        return getUser(username)
            .map(User::entity)
            .filter(userEntity -> !StringUtils.hasText(userEntity.getPassword())
                || !passwordEncoder.matches(rawPassword, userEntity.getPassword()))
            .map(userEntity -> userEntity.setPassword(passwordEncoder.encode(rawPassword)))
            .flatMap(repository::save)
            .map(User::new);
    }
}
