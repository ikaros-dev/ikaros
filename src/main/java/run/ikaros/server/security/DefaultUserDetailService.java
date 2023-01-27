package run.ikaros.server.security;

import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.user.RoleService;
import run.ikaros.server.core.user.User;
import run.ikaros.server.core.user.UserService;

/**
 * ikaros default user detail service.
 *
 * @author: li-guohao
 */
public class DefaultUserDetailService implements ReactiveUserDetailsService {
    private final UserService userService;
    private final RoleService roleService;

    public DefaultUserDetailService(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userService.getUser(username)
            .map(User::entity)
            .flatMap(userEntity -> roleService.findNameById(userEntity.getRoleId())
                .map(role -> org.springframework.security.core.userdetails.User.builder()
                    .username(username)
                    .password(userEntity.getPassword())
                    .roles(role).build()));
    }
}
