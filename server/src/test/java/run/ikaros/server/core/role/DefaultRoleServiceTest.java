package run.ikaros.server.core.role;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.role.Role;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.store.entity.RoleEntity;
import run.ikaros.server.store.repository.RoleRepository;

class DefaultRoleServiceTest {

    private RoleRepository roleRepository;
    private ApplicationEventPublisher applicationEventPublisher;
    private DefaultRoleService defaultRoleService;

    @BeforeEach
    void setUp() {
        roleRepository = Mockito.mock(RoleRepository.class);
        applicationEventPublisher =
            Mockito.mock(ApplicationEventPublisher.class);
        defaultRoleService =
            new DefaultRoleService(roleRepository, applicationEventPublisher);
    }

    @Test
    void findById_found() {
        UUID roleId = UuidV7Utils.generateUuid();
        RoleEntity entity = RoleEntity.builder()
            .name("TEST_ROLE")
            .build();
        entity.setId(roleId);

        when(roleRepository.findById(roleId))
            .thenReturn(Mono.just(entity));

        StepVerifier.create(defaultRoleService.findById(roleId))
            .expectNextMatches(role -> "TEST_ROLE".equals(role.getName())
                && roleId.equals(role.getId()))
            .verifyComplete();
    }

    @Test
    void findById_notFound() {
        UUID roleId = UuidV7Utils.generateUuid();

        when(roleRepository.findById(roleId))
            .thenReturn(Mono.empty());

        StepVerifier.create(defaultRoleService.findById(roleId))
            .verifyComplete();
    }

    @Test
    void findNameById_found() {
        UUID roleId = UuidV7Utils.generateUuid();
        RoleEntity entity = RoleEntity.builder()
            .name("ADMIN")
            .build();
        entity.setId(roleId);

        when(roleRepository.findById(roleId))
            .thenReturn(Mono.just(entity));

        StepVerifier.create(defaultRoleService.findNameById(roleId))
            .expectNext("ADMIN")
            .verifyComplete();
    }

    @Test
    void createIfNotExist_notExist() {
        RoleEntity entity = RoleEntity.builder()
            .name("NEW_ROLE")
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        when(roleRepository.existsByName("NEW_ROLE"))
            .thenReturn(Mono.just(false));
        when(roleRepository.insert(any(RoleEntity.class)))
            .thenReturn(Mono.just(entity));

        StepVerifier.create(
                defaultRoleService.createIfNotExist("NEW_ROLE"))
            .expectNextMatches(
                role -> "NEW_ROLE".equals(role.getName()))
            .verifyComplete();

        verify(applicationEventPublisher).publishEvent(any());
    }

    @Test
    void createIfNotExist_alreadyExist() {
        when(roleRepository.existsByName("EXISTING_ROLE"))
            .thenReturn(Mono.just(true));

        StepVerifier.create(
                defaultRoleService.createIfNotExist("EXISTING_ROLE"))
            .verifyComplete();

        verify(roleRepository, never())
            .insert(any(RoleEntity.class));
        verify(applicationEventPublisher, never())
            .publishEvent(any());
    }

    @Test
    void findAll_multipleRoles() {
        RoleEntity entity1 = RoleEntity.builder()
            .name("ROLE_1").build();
        entity1.setId(UuidV7Utils.generateUuid());
        RoleEntity entity2 = RoleEntity.builder()
            .name("ROLE_2").build();
        entity2.setId(UuidV7Utils.generateUuid());
        RoleEntity entity3 = RoleEntity.builder()
            .name("ROLE_3").build();
        entity3.setId(UuidV7Utils.generateUuid());

        when(roleRepository.findAll())
            .thenReturn(Flux.just(entity1, entity2, entity3));

        StepVerifier.create(defaultRoleService.findAll())
            .expectNextCount(3)
            .verifyComplete();
    }

    @Test
    void deleteById_success() {
        UUID roleId = UuidV7Utils.generateUuid();

        when(roleRepository.deleteById(roleId))
            .thenReturn(Mono.empty());

        StepVerifier.create(defaultRoleService.deleteById(roleId))
            .verifyComplete();

        verify(roleRepository).deleteById(roleId);
    }

    @Test
    void save_newRole() {
        Role role = Role.builder()
            .name("NEW_ROLE")
            .build();

        when(roleRepository.insert(any(RoleEntity.class)))
            .thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0)));

        StepVerifier.create(defaultRoleService.save(role))
            .expectNextMatches(
                r -> "NEW_ROLE".equals(r.getName()))
            .verifyComplete();

        verify(roleRepository)
            .insert(any(RoleEntity.class));
        verify(applicationEventPublisher).publishEvent(any());
    }

    @Test
    void save_existingRole() {
        UUID roleId = UuidV7Utils.generateUuid();
        Role role = Role.builder()
            .name("UPDATED_ROLE")
            .build();
        role.setId(roleId);

        RoleEntity entity = RoleEntity.builder()
            .name("OLD_ROLE")
            .build();
        entity.setId(roleId);

        when(roleRepository.findById(roleId))
            .thenReturn(Mono.just(entity));
        when(roleRepository.update(any(RoleEntity.class)))
            .thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0)));

        StepVerifier.create(defaultRoleService.save(role))
            .expectNextMatches(
                r -> "UPDATED_ROLE".equals(r.getName()))
            .verifyComplete();

        verify(roleRepository).findById(roleId);
        verify(roleRepository)
            .update(any(RoleEntity.class));
    }
}
