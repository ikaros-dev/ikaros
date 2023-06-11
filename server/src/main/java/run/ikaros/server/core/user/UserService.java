package run.ikaros.server.core.user;

import jakarta.annotation.Nonnull;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

public interface UserService {
    String DEFAULT_PASSWORD_ENCODING_ID_PREFIX = "{bcrypt}";

    /**
     * add password default encoding id prefix if not exists.
     *
     * @param rawPassword raw password
     * @return {encodingId} + raw password
     * @see UserService#DEFAULT_PASSWORD_ENCODING_ID_PREFIX
     */
    static String addEncodingIdPrefixIfNotExists(String rawPassword) {
        Assert.hasText(rawPassword, "'rawPassword' must has text");
        return rawPassword.startsWith(DEFAULT_PASSWORD_ENCODING_ID_PREFIX) ? rawPassword :
            DEFAULT_PASSWORD_ENCODING_ID_PREFIX + rawPassword;
    }

    Mono<User> save(User user);

    Mono<Void> deleteAll();

    Mono<User> getUserByUsername(String username);

    Mono<User> updatePassword(String username, String rawPassword);

    Mono<User> update(@Nonnull User user);
}
