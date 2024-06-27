package run.ikaros.server.core.role;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import run.ikaros.server.store.repository.RoleAuthorityRepository;

@Slf4j
@Service
public class DefaultRoleAuthorityService implements RoleAuthorityService {
    private final RoleAuthorityRepository roleAuthorityRepository;

    public DefaultRoleAuthorityService(RoleAuthorityRepository roleAuthorityRepository) {
        this.roleAuthorityRepository = roleAuthorityRepository;
    }
}
