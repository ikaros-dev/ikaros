package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.model.entity.UserRoleEntity;
import cn.liguohao.ikaros.repository.UserRoleRepository;
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
        Assert.isNotNull(id);
        return userRoleRepository.findByUserId(id);
    }
}
