package run.ikaros.server.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.server.core.user.RoleService;
import run.ikaros.server.core.user.User;
import run.ikaros.server.core.user.UserService;
import run.ikaros.server.store.entity.AuthorityEntity;
import run.ikaros.server.store.repository.AuthorityRepository;

/**
 * ikaros default user detail service.
 *
 * @author: li-guohao
 */
public class DefaultUserDetailService implements ReactiveUserDetailsService {
    private final UserService userService;
    private final RoleService roleService;
    private final AuthorityRepository authorityRepository;

    /**
     * Construct.
     */
    public DefaultUserDetailService(UserService userService, RoleService roleService,
                                    AuthorityRepository authorityRepository) {
        this.userService = userService;
        this.roleService = roleService;
        this.authorityRepository = authorityRepository;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userService.getUserByUsername(username)
            .map(User::entity)
            .flatMap(userEntity -> roleService.findNameById(userEntity.getRoleId())
                .flatMap(role ->
                    getAuthorities(userEntity.getRoleId())
                        .collectList()
                        .map(authorities ->
                            org.springframework.security.core.userdetails.User.builder()
                                .username(username)
                                .password(userEntity.getPassword())
                                .authorities(authorities)
                                .build()))
            );
    }

    private Flux<GrantedAuthority> getAuthorities(Long roleId) {
        return authorityRepository.findByRoleId(roleId)
            .filter(AuthorityEntity::getAllow)
            .map(authority -> authority.getTarget()
                + SecurityConst.AUTHORITY_DIVIDE
                + authority.getAuthority())
            .map(IkarosGrantedAuthority::new);
    }

}
