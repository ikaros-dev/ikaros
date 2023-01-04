package run.ikaros.server.core.user;

import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.UserEntity;
import run.ikaros.server.store.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository repository;

    public UserServiceImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<User> save(User user) {
        Assert.notNull(user, "'user' must not be null");
        final UserEntity userEntity = user.entity();
        Assert.notNull(userEntity, "'userEntity' must not be null");
        return repository.save(userEntity)
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
    public Mono<User> updatePassword(String username, String newPassword) {
        Assert.hasText(username, "'username' must has text");
        Assert.hasText(newPassword, "'newPassword' must has text");
        return repository
            .save(getUser(username).block().entity())
            .map(User::new);
    }
}
