package run.ikaros.server.core.user;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.ikaros.server.infra.constant.SecurityConst;

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

    Mono<User> getUser(String username);

    Mono<User> updatePassword(String username, String rawPassword);


    /**
     * Get current login user id.
     *
     * @return user id
     */
    static Long getCurrentLoginUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
            && StringUtils.isNotBlank((String) authentication.getCredentials())) {
            // String token = (String) authentication.getCredentials();
            // return Long.valueOf(
            //     String.valueOf(JwtUtils.getTokenHeaderValue(token, SecurityConst.HEADER_UID)));
            // todo get user info and return uid
        }
        return SecurityConst.UID_WHEN_NO_AUTH;
    }
}
