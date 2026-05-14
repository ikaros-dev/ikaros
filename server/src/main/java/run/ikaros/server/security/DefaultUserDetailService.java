package run.ikaros.server.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import run.ikaros.server.store.entity.User;
import run.ikaros.server.store.mapper.UserMapper;

@Slf4j
public class DefaultUserDetailService implements UserDetailsService {
    private final UserMapper userMapper;

    public DefaultUserDetailService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Load user by username: {}", username);
        User user = userMapper.findByUsername(username);

        return null;
    }
}
