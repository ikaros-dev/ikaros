package run.ikaros.server.core.user;

import reactor.core.publisher.Mono;

public interface UserService {

    Mono<User> save(User user);

    Mono<Void> deleteAll();

    Mono<User> getUser(String username);

    Mono<User> updatePassword(String username, String rawPassword);
}
