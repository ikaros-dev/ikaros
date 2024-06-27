package run.ikaros.server.core.role;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.role.Role;
import run.ikaros.server.core.role.event.RoleCreatedEvent;
import run.ikaros.server.store.entity.RoleEntity;
import run.ikaros.server.store.repository.RoleRepository;

@Slf4j
@Service
public class DefaultRoleService implements RoleService {
    private final RoleRepository roleRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public DefaultRoleService(RoleRepository roleRepository,
                              ApplicationEventPublisher applicationEventPublisher) {
        this.roleRepository = roleRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public Mono<Role> findById(Long roleId) {
        return roleRepository.findById(roleId)
            .map(this::entity2Vo);
    }

    private Role entity2Vo(RoleEntity e) {
        Role role = new Role();
        role.setId(e.getId());
        role.setName(e.getName());
        role.setDescription(e.getDescription());
        role.setParentId(Objects.isNull(e.getParentId()) ? 0L : e.getParentId());
        return role;
    }

    private RoleEntity vo2Entity(Role role) {
        RoleEntity entity = new RoleEntity();
        entity.setId(role.getId());
        entity.setName(role.getName());
        entity.setDescription(role.getDescription());
        entity.setParentId(Objects.isNull(role.getParentId()) ? 0L : role.getParentId());
        return entity;
    }

    @Override
    public Mono<String> findNameById(Long roleId) {
        return findById(roleId)
            .flatMap(role -> Mono.just(role.getName()));
    }

    @Override
    public Mono<Role> createIfNotExist(String role) {
        return roleRepository.existsByName(role)
            .filter(Boolean.FALSE::equals)
            .flatMap(exists -> Mono.just(RoleEntity.builder()
                .name(role)
                .parentId(0L)
                .build()))
            .flatMap(roleEntity ->
                roleRepository.save(roleEntity)
                    .doOnSuccess(entity -> {
                        log.debug("create role if not exists for entity={}", roleEntity);
                        RoleCreatedEvent event = new RoleCreatedEvent(this, roleEntity);
                        applicationEventPublisher.publishEvent(event);
                        log.debug("publish role created event={}", event);
                    }))
            .map(this::entity2Vo);
    }

    @Override
    public Flux<Role> findAll() {
        return roleRepository.findAll()
            .map(this::entity2Vo);
    }

    @Override
    public Mono<Void> deleteById(Long roleId) {
        Assert.isTrue(roleId > 1, "roleId must be greater than 1");
        return roleRepository.deleteById(roleId);
    }

    @Override
    public Mono<Role> save(Role role) {
        Assert.notNull(role, "role must not be null");
        return Mono.just(role)
            .mapNotNull(Role::getId)
            .flatMap(roleRepository::findById)
            .map(entity -> entity.setName(role.getName())
                .setParentId(Objects.isNull(role.getParentId()) ? 0L : role.getParentId())
                .setDescription(role.getDescription()))
            .flatMap(roleRepository::save)
            .doOnNext(roleEntity -> log.debug("update exists role entity={}", roleEntity))
            .switchIfEmpty(createNewRole(role))
            .map(this::entity2Vo);
    }

    private Mono<RoleEntity> createNewRole(Role role) {
        return Mono.just(vo2Entity(role))
            .flatMap(roleRepository::save)
            .doOnNext(roleEntity -> {
                log.debug("create new role entity={}", roleEntity);
                RoleCreatedEvent event = new RoleCreatedEvent(this, roleEntity);
                applicationEventPublisher.publishEvent(event);
                log.debug("publish role created event={}", event);
            });
    }
}
