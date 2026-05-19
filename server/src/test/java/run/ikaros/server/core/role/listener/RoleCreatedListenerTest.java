package run.ikaros.server.core.role.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AuthorityType;
import run.ikaros.server.core.role.event.RoleCreatedEvent;
import run.ikaros.server.store.entity.AuthorityEntity;
import run.ikaros.server.store.entity.RoleAuthorityEntity;
import run.ikaros.server.store.entity.RoleEntity;
import run.ikaros.server.store.repository.AuthorityRepository;
import run.ikaros.server.store.repository.RoleAuthorityRepository;

class RoleCreatedListenerTest {

    private AuthorityRepository authorityRepository;
    private RoleAuthorityRepository roleAuthorityRepository;
    private RoleCreatedListener listener;

    @BeforeEach
    void setUp() {
        authorityRepository = mock(AuthorityRepository.class);
        roleAuthorityRepository = mock(RoleAuthorityRepository.class);
        listener = new RoleCreatedListener(authorityRepository, roleAuthorityRepository);
    }

    @Test
    void onRoleCreated_masterRole_addsAuthority() {
        // Given
        UUID roleId = UuidV7Utils.generateUuid();
        RoleEntity roleEntity = RoleEntity.builder()
            .name(SecurityConst.ROLE_MASTER)
            .description("Default admin role")
            .build();
        roleEntity.setId(roleId);
        RoleCreatedEvent event = new RoleCreatedEvent(this, roleEntity);

        UUID authorityId = UuidV7Utils.generateUuid();
        AuthorityEntity authorityEntity = AuthorityEntity.builder()
            .type(AuthorityType.ALL)
            .target(SecurityConst.Authorization.Target.ALL)
            .authority(SecurityConst.Authorization.Authority.ALL)
            .build();
        authorityEntity.setId(authorityId);

        when(authorityRepository.findByTypeAndTargetAndAuthority(
                AuthorityType.ALL,
                SecurityConst.Authorization.Target.ALL,
                SecurityConst.Authorization.Authority.ALL))
            .thenReturn(Mono.just(authorityEntity));

        RoleAuthorityEntity savedEntity = RoleAuthorityEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .authorityId(authorityId)
            .roleId(roleId)
            .build();
        when(roleAuthorityRepository.insert(
                ArgumentMatchers.any(RoleAuthorityEntity.class)))
            .thenReturn(Mono.just(savedEntity));

        // When & Then
        StepVerifier.create(listener.onRoleCreated(event))
            .verifyComplete();

        verify(authorityRepository).findByTypeAndTargetAndAuthority(
            AuthorityType.ALL,
            SecurityConst.Authorization.Target.ALL,
            SecurityConst.Authorization.Authority.ALL);
        verify(roleAuthorityRepository).insert(
            ArgumentMatchers.any(RoleAuthorityEntity.class));
    }

    @Test
    void onRoleCreated_nonMasterRole_doesNothing() {
        // Given
        UUID roleId = UuidV7Utils.generateUuid();
        RoleEntity roleEntity = RoleEntity.builder()
            .name("USER")
            .description("Regular user role")
            .build();
        roleEntity.setId(roleId);
        RoleCreatedEvent event = new RoleCreatedEvent(this, roleEntity);

        // When & Then
        StepVerifier.create(listener.onRoleCreated(event))
            .verifyComplete();

        verify(authorityRepository, never()).findByTypeAndTargetAndAuthority(
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any());
    }
}
