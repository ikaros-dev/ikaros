package run.ikaros.server.core.role;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AuthorityType;
import run.ikaros.server.store.entity.AuthorityEntity;
import run.ikaros.server.store.entity.RoleAuthorityEntity;
import run.ikaros.server.store.repository.AuthorityRepository;
import run.ikaros.server.store.repository.RoleAuthorityRepository;

class DefaultRoleAuthorityServiceTest {

    private RoleAuthorityRepository roleAuthorityRepository;
    private AuthorityRepository authorityRepository;
    private DefaultRoleAuthorityService defaultRoleAuthorityService;

    @BeforeEach
    void setUp() {
        roleAuthorityRepository =
            Mockito.mock(RoleAuthorityRepository.class);
        authorityRepository =
            Mockito.mock(AuthorityRepository.class);
        defaultRoleAuthorityService =
            new DefaultRoleAuthorityService(
                roleAuthorityRepository, authorityRepository);
    }

    @Test
    void addAuthoritiesForRole_multipleAuthorities() {
        UUID roleId = UuidV7Utils.generateUuid();
        UUID authId1 = UuidV7Utils.generateUuid();
        UUID authId2 = UuidV7Utils.generateUuid();

        when(roleAuthorityRepository.insert(
            any(RoleAuthorityEntity.class)))
            .thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0)));

        AuthorityEntity ae = AuthorityEntity.builder()
            .type(AuthorityType.ALL)
            .target("*")
            .authority("*")
            .build();
        ae.setId(UuidV7Utils.generateUuid());

        when(authorityRepository.findById(any(UUID.class)))
            .thenReturn(Mono.just(ae));

        StepVerifier.create(
                defaultRoleAuthorityService
                    .addAuthoritiesForRole(
                        roleId, new UUID[] {authId1, authId2}))
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    void deleteAuthoritiesForRole_success() {
        UUID roleId = UuidV7Utils.generateUuid();
        UUID authId1 = UuidV7Utils.generateUuid();
        UUID authId2 = UuidV7Utils.generateUuid();

        when(roleAuthorityRepository
            .deleteByRoleIdAndAuthorityId(
                any(UUID.class), any(UUID.class)))
            .thenReturn(Mono.just(true));

        StepVerifier.create(
                defaultRoleAuthorityService
                    .deleteAuthoritiesForRole(
                        roleId, new UUID[] {authId1, authId2}))
            .verifyComplete();
    }

    @Test
    void getAuthoritiesForRole_found() {
        UUID roleId = UuidV7Utils.generateUuid();
        UUID authId1 = UuidV7Utils.generateUuid();
        UUID authId2 = UuidV7Utils.generateUuid();

        RoleAuthorityEntity rae1 =
            RoleAuthorityEntity.builder()
                .id(UuidV7Utils.generateUuid())
                .roleId(roleId)
                .authorityId(authId1)
                .build();
        RoleAuthorityEntity rae2 =
            RoleAuthorityEntity.builder()
                .id(UuidV7Utils.generateUuid())
                .roleId(roleId)
                .authorityId(authId2)
                .build();

        AuthorityEntity ae = AuthorityEntity.builder()
            .type(AuthorityType.ALL)
            .target("*")
            .authority("*")
            .build();
        ae.setId(UuidV7Utils.generateUuid());

        when(roleAuthorityRepository.findByRoleId(roleId))
            .thenReturn(Flux.just(rae1, rae2));
        when(authorityRepository.findById(any(UUID.class)))
            .thenReturn(Mono.just(ae));

        StepVerifier.create(
                defaultRoleAuthorityService
                    .getAuthoritiesForRole(roleId))
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    void getAuthoritiesForRole_empty() {
        UUID roleId = UuidV7Utils.generateUuid();

        when(roleAuthorityRepository.findByRoleId(roleId))
            .thenReturn(Flux.empty());

        StepVerifier.create(
                defaultRoleAuthorityService
                    .getAuthoritiesForRole(roleId))
            .verifyComplete();
    }
}
