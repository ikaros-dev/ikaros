package run.ikaros.server.core.user.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.role.Role;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.store.entity.RoleEntity;
import run.ikaros.server.store.entity.UserRoleEntity;
import run.ikaros.server.store.repository.RoleRepository;
import run.ikaros.server.store.repository.UserRoleRepository;

class DefaultUserRoleServiceTest {

    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private RoleRepository roleRepository;
    private DefaultUserRoleService defaultUserRoleService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        defaultUserRoleService = new DefaultUserRoleService(userRoleRepository, roleRepository);
    }

    @Test
    void saveEntity_newEntity() {
        UUID userId = UuidV7Utils.generateUuid();
        UUID roleId = UuidV7Utils.generateUuid();
        UserRoleEntity entity = UserRoleEntity.builder()
            .userId(userId)
            .roleId(roleId)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        when(userRoleRepository.findByUserIdAndRoleId(userId, roleId))
            .thenReturn(Mono.empty());
        when(userRoleRepository.save(any(UserRoleEntity.class)))
            .thenReturn(Mono.just(entity));

        StepVerifier.create(defaultUserRoleService.saveEntity(entity))
            .assertNext(saved -> {
                assertThat(saved.getUserId()).isEqualTo(userId);
                assertThat(saved.getRoleId()).isEqualTo(roleId);
            })
            .verifyComplete();

        verify(userRoleRepository).findByUserIdAndRoleId(userId, roleId);
        verify(userRoleRepository).save(entity);
    }

    @Test
    void saveEntity_existingEntity() {
        UUID userId = UuidV7Utils.generateUuid();
        UUID roleId = UuidV7Utils.generateUuid();
        UserRoleEntity existingEntity = UserRoleEntity.builder()
            .userId(userId)
            .roleId(roleId)
            .build();
        existingEntity.setId(UuidV7Utils.generateUuid());

        UserRoleEntity incomingEntity = UserRoleEntity.builder()
            .userId(userId)
            .roleId(roleId)
            .build();
        incomingEntity.setId(UuidV7Utils.generateUuid());

        when(userRoleRepository.findByUserIdAndRoleId(userId, roleId))
            .thenReturn(Mono.just(existingEntity));
        when(userRoleRepository.save(any(UserRoleEntity.class)))
            .thenReturn(Mono.just(existingEntity));

        StepVerifier.create(defaultUserRoleService.saveEntity(incomingEntity))
            .assertNext(saved -> {
                assertThat(saved.getUserId()).isEqualTo(userId);
                assertThat(saved.getRoleId()).isEqualTo(roleId);
            })
            .verifyComplete();

        verify(userRoleRepository).findByUserIdAndRoleId(userId, roleId);
        verify(userRoleRepository).save(any(UserRoleEntity.class));
    }

    @Test
    void addUserRoles_multipleRoles() {
        UUID userId = UuidV7Utils.generateUuid();
        UUID roleId1 = UuidV7Utils.generateUuid();
        UUID roleId2 = UuidV7Utils.generateUuid();
        UUID[] roleIds = new UUID[]{roleId1, roleId2};

        // For each roleId, findByUserIdAndRoleId returns empty -> insert is called
        when(userRoleRepository.findByUserIdAndRoleId(userId, roleId1))
            .thenReturn(Mono.empty());
        when(userRoleRepository.findByUserIdAndRoleId(userId, roleId2))
            .thenReturn(Mono.empty());

        UserRoleEntity userRoleEntity1 = UserRoleEntity.builder()
            .userId(userId).roleId(roleId1).build();
        userRoleEntity1.setId(UuidV7Utils.generateUuid());
        UserRoleEntity userRoleEntity2 = UserRoleEntity.builder()
            .userId(userId).roleId(roleId2).build();
        userRoleEntity2.setId(UuidV7Utils.generateUuid());

        when(userRoleRepository.insert(any(UserRoleEntity.class)))
            .thenReturn(Mono.just(userRoleEntity1))
            .thenReturn(Mono.just(userRoleEntity2));

        RoleEntity roleEntity1 = RoleEntity.builder().name("ADMIN").build();
        roleEntity1.setId(roleId1);
        RoleEntity roleEntity2 = RoleEntity.builder().name("USER").build();
        roleEntity2.setId(roleId2);

        when(roleRepository.findById(roleId1)).thenReturn(Mono.just(roleEntity1));
        when(roleRepository.findById(roleId2)).thenReturn(Mono.just(roleEntity2));

        StepVerifier.create(defaultUserRoleService.addUserRoles(userId, roleIds))
            .assertNext(role -> {
                assertThat(role.getId()).isEqualTo(roleId1);
                assertThat(role.getName()).isEqualTo("ADMIN");
            })
            .assertNext(role -> {
                assertThat(role.getId()).isEqualTo(roleId2);
                assertThat(role.getName()).isEqualTo("USER");
            })
            .verifyComplete();

        verify(userRoleRepository).findByUserIdAndRoleId(userId, roleId1);
        verify(userRoleRepository).findByUserIdAndRoleId(userId, roleId2);
        verify(userRoleRepository, org.mockito.Mockito.times(2))
            .insert(any(UserRoleEntity.class));
        verify(roleRepository).findById(roleId1);
        verify(roleRepository).findById(roleId2);
    }

    @Test
    void deleteUserRoles_success() {
        UUID userId = UuidV7Utils.generateUuid();
        UUID roleId1 = UuidV7Utils.generateUuid();
        UUID roleId2 = UuidV7Utils.generateUuid();
        UUID[] roleIds = new UUID[]{roleId1, roleId2};

        when(userRoleRepository.deleteByUserIdAndRoleId(userId, roleId1))
            .thenReturn(Mono.empty());
        when(userRoleRepository.deleteByUserIdAndRoleId(userId, roleId2))
            .thenReturn(Mono.empty());

        StepVerifier.create(defaultUserRoleService.deleteUserRoles(userId, roleIds))
            .verifyComplete();

        verify(userRoleRepository).deleteByUserIdAndRoleId(userId, roleId1);
        verify(userRoleRepository).deleteByUserIdAndRoleId(userId, roleId2);
    }

    @Test
    void getRolesForUser_found() {
        UUID userId = UuidV7Utils.generateUuid();
        UUID roleId1 = UuidV7Utils.generateUuid();
        UUID roleId2 = UuidV7Utils.generateUuid();

        UserRoleEntity userRoleEntity1 = UserRoleEntity.builder()
            .userId(userId).roleId(roleId1).build();
        userRoleEntity1.setId(UuidV7Utils.generateUuid());
        UserRoleEntity userRoleEntity2 = UserRoleEntity.builder()
            .userId(userId).roleId(roleId2).build();
        userRoleEntity2.setId(UuidV7Utils.generateUuid());

        when(userRoleRepository.findByUserId(userId))
            .thenReturn(Flux.just(userRoleEntity1, userRoleEntity2));

        RoleEntity roleEntity1 = RoleEntity.builder().name("ADMIN").build();
        roleEntity1.setId(roleId1);
        RoleEntity roleEntity2 = RoleEntity.builder().name("USER").build();
        roleEntity2.setId(roleId2);

        when(roleRepository.findById(roleId1)).thenReturn(Mono.just(roleEntity1));
        when(roleRepository.findById(roleId2)).thenReturn(Mono.just(roleEntity2));

        StepVerifier.create(defaultUserRoleService.getRolesForUser(userId))
            .assertNext(role -> {
                assertThat(role.getId()).isEqualTo(roleId1);
                assertThat(role.getName()).isEqualTo("ADMIN");
            })
            .assertNext(role -> {
                assertThat(role.getId()).isEqualTo(roleId2);
                assertThat(role.getName()).isEqualTo("USER");
            })
            .verifyComplete();

        verify(userRoleRepository).findByUserId(userId);
        verify(roleRepository).findById(roleId1);
        verify(roleRepository).findById(roleId2);
    }

    @Test
    void getRolesForUser_empty() {
        UUID userId = UuidV7Utils.generateUuid();

        when(userRoleRepository.findByUserId(userId))
            .thenReturn(Flux.empty());

        StepVerifier.create(defaultUserRoleService.getRolesForUser(userId))
            .verifyComplete();

        verify(userRoleRepository).findByUserId(userId);
        verify(roleRepository, never()).findById(any(UUID.class));
    }
}
