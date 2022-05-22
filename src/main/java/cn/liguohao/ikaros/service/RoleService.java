package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.entity.Role;
import cn.liguohao.ikaros.repository.RoleRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author liguohao
 */
@Component
public class RoleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleService.class);

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }


    public Role save(Role role) {
        return roleRepository.save(role);
    }

    public Optional<Role> findById(Long roleId) {
        return roleRepository.findById(roleId);
    }
}
