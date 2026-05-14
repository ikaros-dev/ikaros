package run.ikaros.server.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import run.ikaros.server.store.entity.*;
import run.ikaros.server.store.mapper.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DefaultUserDetailService implements UserDetailsService {
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RoleAuthorityMapper roleAuthorityMapper;
    private final AuthorityMapper authorityMapper;

    public DefaultUserDetailService(UserMapper userMapper, UserRoleMapper userRoleMapper,
                                    RoleMapper roleMapper, RoleAuthorityMapper roleAuthorityMapper,
                                    AuthorityMapper authorityMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.roleAuthorityMapper = roleAuthorityMapper;
        this.authorityMapper = authorityMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Load user by username: {}", username);
        User ikuser = userMapper.findByUsernameAndEnableAndDeleteStatus(username, true, false);
        UserRole userRole = userRoleMapper.findByUserId(ikuser.getId());
        Role role = roleMapper.selectById(userRole.getRoleId());
        log.debug("Current user[{}] has role: {}", username, role.getName());
        List<RoleAuthority> roleAuthorities = roleAuthorityMapper.findAllByRoleId(role.getId());
        List<IkarosGrantedAuthority> authorities = new ArrayList<>(roleAuthorities.size());
        for (RoleAuthority roleAuthority : roleAuthorities) {
            Authority authority = authorityMapper.selectById(roleAuthority.getAuthorityId());
            IkarosGrantedAuthority grantedAuthority = new IkarosGrantedAuthority(authority);
            authorities.add(grantedAuthority);
        }
        log.debug("Current user[{}] has authorities: {}", username, authorities);
        return new org.springframework.security.core.userdetails.User(
                ikuser.getUsername(), ikuser.getPassword(), authorities
        );
    }
}
