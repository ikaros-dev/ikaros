package run.ikaros.server.security;

import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.user.User;
import run.ikaros.server.core.user.UserService;
import run.ikaros.server.infra.constant.SecurityConst;

/**
 * ikaros default user detail service.
 *
 * @author: li-guohao
 */
public class DefaultUserDetailService implements ReactiveUserDetailsService {
    private final UserService userService;

    public DefaultUserDetailService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userService.getUser(username)
            .map(User::entity)
            .map(userEntity -> org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password(userEntity.getPassword())
                .roles(SecurityConst.DEFAULT_ROLE).build());
    }
}
