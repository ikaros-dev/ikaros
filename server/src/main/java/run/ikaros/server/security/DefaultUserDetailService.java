package run.ikaros.server.security;

import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.security.Authority;
import run.ikaros.server.store.entity.BaseEntity;
import run.ikaros.server.store.entity.RoleAuthorityEntity;
import run.ikaros.server.store.entity.UserEntity;
import run.ikaros.server.store.entity.UserRoleEntity;
import run.ikaros.server.store.repository.AuthorityRepository;
import run.ikaros.server.store.repository.RoleAuthorityRepository;
import run.ikaros.server.store.repository.UserRepository;
import run.ikaros.server.store.repository.UserRoleRepository;

/**
 * ikaros default user detail service.
 *
 * @author: chivehao
 */
public class DefaultUserDetailService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuthorityRepository authorityRepository;
    private final RoleAuthorityRepository roleAuthorityRepository;

    /**
     * Construct.
     */
    public DefaultUserDetailService(UserRepository userRepository,
                                    UserRoleRepository userRoleRepository,
                                    AuthorityRepository authorityRepository,
                                    RoleAuthorityRepository roleAuthorityRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.authorityRepository = authorityRepository;
        this.roleAuthorityRepository = roleAuthorityRepository;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository
            .findByUsernameAndEnableAndDeleteStatus(username, true, false)
            .map(BaseEntity::getId)
            .flatMapMany(userRoleRepository::findByUserId)
            .map(UserRoleEntity::getRoleId)
            .flatMap(roleId -> roleAuthorityRepository.findByRoleId(roleId).collectList())
            .flatMap(Flux::fromIterable)
            .collectList()
            .flatMapMany(Flux::fromIterable)
            .map(RoleAuthorityEntity::getAuthorityId)
            .flatMap(authorityRepository::findById)
            .flatMap(authorityEntity -> copyProperties(authorityEntity, new Authority()))
            .map(IkarosGrantedAuthority::new)
            .collectList()
            .flatMap(authorities ->
                userRepository.findByUsernameAndEnableAndDeleteStatus(username, true, false)
                    .map(UserEntity::getPassword)
                    .map(password -> User.builder()
                        .authorities(authorities)
                        .username(username)
                        .password(password)
                        .build()));
    }

}
