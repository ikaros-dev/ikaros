package run.ikaros.server.core.user;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.RoleEntity;
import run.ikaros.server.store.repository.RoleRepository;

@Service
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Mono<RoleEntity> findById(Long roleId) {
        return roleRepository.findById(roleId);
    }

    @Override
    public Mono<String> findNameById(Long roleId) {
        return findById(roleId)
            .flatMap(role -> Mono.just(role.getName()));
    }

    @Override
    public Mono<RoleEntity> createIfNotExist(String role) {
        return roleRepository.existsByName(role)
            .filter(Boolean.FALSE::equals)
            .flatMap(exists -> Mono.just(RoleEntity.builder()
                .name(role)
                .parentId(0L)
                .build()))
            .flatMap(roleRepository::save);
    }
}
