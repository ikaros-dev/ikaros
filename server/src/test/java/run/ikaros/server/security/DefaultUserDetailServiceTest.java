package run.ikaros.server.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AuthorityType;
import run.ikaros.server.store.entity.AuthorityEntity;
import run.ikaros.server.store.entity.RoleAuthorityEntity;
import run.ikaros.server.store.entity.UserEntity;
import run.ikaros.server.store.entity.UserRoleEntity;
import run.ikaros.server.store.repository.AuthorityRepository;
import run.ikaros.server.store.repository.RoleAuthorityRepository;
import run.ikaros.server.store.repository.UserRepository;
import run.ikaros.server.store.repository.UserRoleRepository;

class DefaultUserDetailServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private AuthorityRepository authorityRepository;
    @Mock
    private RoleAuthorityRepository roleAuthorityRepository;
    private DefaultUserDetailService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DefaultUserDetailService(
            userRepository, userRoleRepository, authorityRepository, roleAuthorityRepository);
    }

    @Test
    void findByUsername_withAuthorities() {
        String username = "testuser";
        String encodedPassword = "encodedpass";
        UUID userId = UuidV7Utils.generateUuid();
        UUID roleId = UuidV7Utils.generateUuid();
        UUID authorityId = UuidV7Utils.generateUuid();

        // Setup user entity
        UserEntity userEntity = UserEntity.builder()
            .username(username)
            .password(encodedPassword)
            .enable(true)
            .build();
        userEntity.setId(userId);

        // Setup user role entity
        UserRoleEntity userRoleEntity = UserRoleEntity.builder()
            .userId(userId)
            .roleId(roleId)
            .build();
        userRoleEntity.setId(UuidV7Utils.generateUuid());

        // Setup role authority entity
        RoleAuthorityEntity roleAuthorityEntity = RoleAuthorityEntity.builder()
            .roleId(roleId)
            .authorityId(authorityId)
            .build();
        roleAuthorityEntity.setId(UuidV7Utils.generateUuid());

        // Setup authority entity
        AuthorityEntity authorityEntity = AuthorityEntity.builder()
            .type(AuthorityType.ALL)
            .target("*")
            .authority("*")
            .build();
        authorityEntity.setId(authorityId);

        // Mock repository calls
        // userRepository.findByUsernameAndEnableAndDeleteStatus is called twice
        when(userRepository.findByUsernameAndEnableAndDeleteStatus(username, true, false))
            .thenReturn(Mono.just(userEntity));

        when(userRoleRepository.findByUserId(userId))
            .thenReturn(Flux.just(userRoleEntity));

        when(roleAuthorityRepository.findByRoleId(roleId))
            .thenReturn(Flux.just(roleAuthorityEntity));

        when(authorityRepository.findById(authorityId))
            .thenReturn(Mono.just(authorityEntity));

        StepVerifier.create(service.findByUsername(username))
            .assertNext(userDetails -> {
                assertThat(userDetails).isNotNull();
                assertThat(userDetails.getUsername()).isEqualTo(username);
                assertThat(userDetails.getPassword()).isEqualTo(encodedPassword);
                assertThat(userDetails.getAuthorities()).hasSize(1);
                String authority = userDetails.getAuthorities().iterator().next().getAuthority();
                assertThat(authority).isEqualTo("ALL&&&&&&*&&&&&&*");
            })
            .verifyComplete();
    }

    @Test
    void findByUsername_noRoles() {
        String username = "testuser";
        String encodedPassword = "encodedpass";
        UUID userId = UuidV7Utils.generateUuid();

        UserEntity userEntity = UserEntity.builder()
            .username(username)
            .password(encodedPassword)
            .enable(true)
            .build();
        userEntity.setId(userId);

        // userRepository.findByUsernameAndEnableAndDeleteStatus is called twice
        when(userRepository.findByUsernameAndEnableAndDeleteStatus(username, true, false))
            .thenReturn(Mono.just(userEntity));

        // No roles for this user
        when(userRoleRepository.findByUserId(userId))
            .thenReturn(Flux.empty());

        StepVerifier.create(service.findByUsername(username))
            .assertNext(userDetails -> {
                assertThat(userDetails).isNotNull();
                assertThat(userDetails.getUsername()).isEqualTo(username);
                assertThat(userDetails.getPassword()).isEqualTo(encodedPassword);
                assertThat(userDetails.getAuthorities()).isEmpty();
            })
            .verifyComplete();
    }
}
