package cn.liguohao.ikaros.handler;

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
public class RoleHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleHandler.class);

    private final RoleRepository roleRepository;

    public RoleHandler(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }


    public Role save(Role role) {
        return roleRepository.save(role);
    }

    public Optional<Role> findById(Long roleId) {
        return roleRepository.findById(roleId);
    }
}
