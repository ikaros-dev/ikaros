package run.ikaros.server.core.user;

import org.springframework.stereotype.Service;
import run.ikaros.server.store.mapper.UserMapper;

@Service
public class DefaultUserService implements UserService {
    private final UserMapper userMapper;

    public DefaultUserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
}
