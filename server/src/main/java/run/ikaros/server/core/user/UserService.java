package run.ikaros.server.core.user;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.user.enums.VerificationCodeType;

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

    Mono<Long> count();

    Mono<Void> deleteAll();

    Mono<User> getUserByUsername(String username);

    Mono<Boolean> existsByUsername(String username);

    Mono<Boolean> existsByEmail(String email);

    Mono<Void> updateUsername(Long id, String username);

    Mono<Void> updatePassword(@NotBlank String username, @NotBlank String oldRawPassword,
                              @NotBlank String rawPassword);

    Mono<User> update(@Nonnull UpdateUserRequest updateUserRequest);

    Mono<Void> bindEmail(@NotBlank String username, @NotBlank String email,
                         @NotBlank String verificationCode);

    Mono<Void> bindTelephone(@NotBlank String username, @NotBlank String telephone,
                             @NotBlank String verificationCode);

    Mono<Void> changeRole(@NotBlank String username, Long roleId);

    Mono<Void> sendVerificationCode(Long userId, VerificationCodeType type);
}
