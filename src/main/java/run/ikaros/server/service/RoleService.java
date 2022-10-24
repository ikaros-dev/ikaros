package run.ikaros.server.service;

import run.ikaros.server.entity.RoleEntity;
import run.ikaros.server.repository.RoleRepository;
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
        return roleRepository.saveAndFlush(roleEntity);
    }

    public Optional<RoleEntity> findById(Long roleId) {
        return roleRepository.findById(roleId);
    }
}
