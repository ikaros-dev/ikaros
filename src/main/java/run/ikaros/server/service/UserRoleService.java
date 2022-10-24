package run.ikaros.server.service;

import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.entity.UserRoleEntity;
import run.ikaros.server.repository.UserRoleRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author li-guohao
 */
@Service
public class UserRoleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRoleService.class);

    private final UserRoleRepository userRoleRepository;

    public UserRoleService(UserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }


    public UserRoleEntity save(UserRoleEntity userRoleEntity) {
        return userRoleRepository.save(userRoleEntity);
    }

    public List<UserRoleEntity> findByUserId(Long id) {
        AssertUtils.notNull(id, "'id' must not be null");
        return userRoleRepository.findByUserId(id);
    }
}
