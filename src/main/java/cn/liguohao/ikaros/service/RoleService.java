package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.model.entity.RoleEntity;
import cn.liguohao.ikaros.repository.RoleRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author liguohao
 */
@Service
public class RoleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleService.class);

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }


    public RoleEntity save(RoleEntity roleEntity) {
        return roleRepository.save(roleEntity);
    }

    public Optional<RoleEntity> findById(Long roleId) {
        return roleRepository.findById(roleId);
    }
}
