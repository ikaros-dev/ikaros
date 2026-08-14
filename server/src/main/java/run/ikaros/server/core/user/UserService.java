package run.ikaros.server.core.user;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
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

    Mono<User> insert(@Nullable User user);

    Mono<Long> count();

    Mono<Void> deleteAll();

    Mono<User> getUserByUsername(String username);

    Mono<Boolean> existsByUsername(String username);

    Mono<Boolean> existsByEmail(String email);

    Mono<Void> updateUsername(UUID id, String username);

    Mono<Void> updatePassword(@NotBlank String username, @NotBlank String oldRawPassword,
                              @NotBlank String rawPassword);

    Mono<User> update(UpdateUserRequest updateUserRequest);

    @Nullable Mono<Void> bindEmail(@NotBlank String username, @NotBlank String email,
                                   @NotBlank String verificationCode);

    @Nullable Mono<Void> bindTelephone(@NotBlank String username, @NotBlank String telephone,
                                       @NotBlank String verificationCode);

    Mono<Void> changeRole(@Nullable @NotBlank String username, UUID roleId);

    Mono<Void> sendVerificationCode(@Nullable UUID userId, VerificationCodeType type);

    Mono<User> create(@Nullable CreateUserReqParams createUserReqParams);

    Flux<User> findAll();

    Mono<Void> deleteById(@Nullable UUID id);

    Mono<User> getUserFromSecurityContext();

    Mono<UUID> getUserIdFromSecurityContext();
}
