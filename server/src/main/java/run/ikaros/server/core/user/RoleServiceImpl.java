package run.ikaros.server.core.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.user.event.RoleCreatedEvent;
import run.ikaros.server.store.entity.RoleEntity;
import run.ikaros.server.store.repository.RoleRepository;

@Slf4j
@Service
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public RoleServiceImpl(RoleRepository roleRepository,
                           ApplicationEventPublisher applicationEventPublisher) {
        this.roleRepository = roleRepository;
        this.applicationEventPublisher = applicationEventPublisher;
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
            .flatMap(roleEntity ->
                roleRepository.save(roleEntity)
                    .doOnSuccess(entity  -> {
                        log.debug("create role if not exists for entity={}", roleEntity);
                        RoleCreatedEvent event = new RoleCreatedEvent(this, roleEntity);
                        applicationEventPublisher.publishEvent(event);
                        log.debug("publish role created event={}", event);
                    }));
    }
}
