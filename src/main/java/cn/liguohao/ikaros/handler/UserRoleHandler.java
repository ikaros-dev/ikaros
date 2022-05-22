package cn.liguohao.ikaros.handler;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.entity.UserRole;
import cn.liguohao.ikaros.repository.UserRoleRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author li-guohao
 */
@Component
public class UserRoleHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRoleHandler.class);

    private final UserRoleRepository userRoleRepository;

    public UserRoleHandler(UserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }


    public UserRole save(UserRole userRole) {
        return userRoleRepository.save(userRole);
    }

    public List<UserRole> findByUserId(Long id) {
        Assert.isNotNull(id);
        return userRoleRepository.findByUserId(id);
    }
}
