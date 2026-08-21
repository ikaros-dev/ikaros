package run.ikaros.server.core.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Example;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.infra.exception.security.PasswordNotMatchingException;
import run.ikaros.api.infra.exception.user.UserExistsException;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.store.entity.UserEntity;
import run.ikaros.server.store.repository.RoleRepository;
import run.ikaros.server.store.repository.UserRepository;

class UserServiceImplTest {

    @Mock
    private UserRepository repository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserServiceImpl(
            repository, roleRepository, passwordEncoder, applicationEventPublisher);
    }

    @Test
    void insert_success() {
        UUID userId = UuidV7Utils.generateUuid();
        UserEntity entity = UserEntity.builder()
            .username("testuser")
            .password("rawpassword")
            .enable(true)
            .build();
        entity.setId(userId);

        when(passwordEncoder.encode("rawpassword")).thenReturn("encodedpassword");
        when(repository.insert(any(UserEntity.class)))
            .thenReturn(Mono.just(entity));

        User user = new User(entity);

        StepVerifier.create(userService.insert(user))
            .assertNext(u -> {
                assertThat(u.entity().getUsername()).isEqualTo("testuser");
                assertThat(u.entity().getPassword()).isEqualTo("encodedpassword");
            })
            .verifyComplete();
    }

    @Test
    void insert_nullUser_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> userService.insert(null));
    }

    @Test
    void count_returnsCount() {
        when(repository.count()).thenReturn(Mono.just(5L));

        StepVerifier.create(userService.count())
            .assertNext(count -> assertThat(count).isEqualTo(5L))
            .verifyComplete();
    }

    @Test
    void deleteAll_success() {
        when(repository.deleteAll()).thenReturn(Mono.empty());

        StepVerifier.create(userService.deleteAll())
            .verifyComplete();
    }

    @Test
    void getUserByUsername_found() {
        String username = "testuser";
        UserEntity entity = UserEntity.builder()
            .username(username)
            .password("pass")
            .enable(true)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        when(repository.findOne(any(Example.class)))
            .thenReturn(Mono.just(entity));

        StepVerifier.create(userService.getUserByUsername(username))
            .assertNext(user -> {
                assertThat(user.entity().getUsername()).isEqualTo(username);
            })
            .verifyComplete();
    }

    @Test
    void getUserByUsername_notFound() {
        when(repository.findOne(any(Example.class)))
            .thenReturn(Mono.empty());

        StepVerifier.create(userService.getUserByUsername("nonexistent"))
            .verifyError();
    }

    @Test
    void getUserByUsername_emptyUsername_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> userService.getUserByUsername(""));
    }

    @Test
    void existsByUsername_true() {
        when(repository.existsByUsername("testuser")).thenReturn(Mono.just(true));

        StepVerifier.create(userService.existsByUsername("testuser"))
            .assertNext(exists -> assertThat(exists).isTrue())
            .verifyComplete();
    }

    @Test
    void existsByEmail_false() {
        when(repository.existsByEmail("test@example.com")).thenReturn(Mono.just(false));

        StepVerifier.create(userService.existsByEmail("test@example.com"))
            .assertNext(exists -> assertThat(exists).isFalse())
            .verifyComplete();
    }

    @Test
    void create_success() {
        String username = "newuser";
        String password = "password123";
        CreateUserReqParams params = new CreateUserReqParams();
        params.setUsername(username);
        params.setPassword(password);
        params.setEnabled(true);

        UserEntity savedEntity = UserEntity.builder()
            .username(username)
            .password("encodedpassword")
            .enable(true)
            .build();
        savedEntity.setId(UuidV7Utils.generateUuid());

        when(passwordEncoder.encode(password)).thenReturn("encodedpassword");
        when(repository.save(any(UserEntity.class))).thenReturn(Mono.just(savedEntity));

        StepVerifier.create(userService.create(params))
            .assertNext(user -> {
                assertThat(user.entity().getUsername()).isEqualTo(username);
                assertThat(user.entity().getPassword()).isEqualTo("encodedpassword");
            })
            .verifyComplete();
    }

    @Test
    void create_nullParams_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> userService.create(null));
    }

    @Test
    void create_duplicateKey_throwsUserExistsException() {
        CreateUserReqParams params = new CreateUserReqParams();
        params.setUsername("existinguser");
        params.setPassword("password");

        when(passwordEncoder.encode("password")).thenReturn("encodedpassword");
        when(repository.save(any(UserEntity.class)))
            .thenReturn(Mono.error(
                new org.springframework.dao.DuplicateKeyException("duplicate key")));

        StepVerifier.create(userService.create(params))
            .verifyError(UserExistsException.class);
    }

    @Test
    void create_enabledFalseByDefault() {
        CreateUserReqParams params = new CreateUserReqParams();
        params.setUsername("newuser");
        params.setPassword("password");

        UserEntity savedEntity = UserEntity.builder()
            .username("newuser")
            .password("encodedpassword")
            .enable(false)
            .build();
        savedEntity.setId(UuidV7Utils.generateUuid());

        when(passwordEncoder.encode("password")).thenReturn("encodedpassword");
        when(repository.save(any(UserEntity.class))).thenReturn(Mono.just(savedEntity));

        StepVerifier.create(userService.create(params))
            .assertNext(user -> {
                assertThat(user.entity().getEnable()).isFalse();
            })
            .verifyComplete();
    }

    @Test
    void findAll_multipleUsers() {
        UserEntity entity1 = UserEntity.builder().username("user1").password("p1").build();
        entity1.setId(UuidV7Utils.generateUuid());
        UserEntity entity2 = UserEntity.builder().username("user2").password("p2").build();
        entity2.setId(UuidV7Utils.generateUuid());

        when(repository.findAll()).thenReturn(Flux.just(entity1, entity2));

        StepVerifier.create(userService.findAll())
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    void deleteById_success() {
        UUID id = UuidV7Utils.generateUuid();
        when(repository.deleteById(id)).thenReturn(Mono.empty());

        StepVerifier.create(userService.deleteById(id))
            .verifyComplete();
    }

    @Test
    void deleteById_nullId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> userService.deleteById(null));
    }

    @Test
    void updateUsername_success() {
        UUID id = UuidV7Utils.generateUuid();
        UserEntity entity = UserEntity.builder()
            .username("oldname")
            .password("pass")
            .build();
        entity.setId(id);

        when(repository.findById(id)).thenReturn(Mono.just(entity));
        when(repository.save(any(UserEntity.class))).thenReturn(Mono.just(entity));

        StepVerifier.create(userService.updateUsername(id, "newname"))
            .verifyComplete();
    }

    @Test
    void updateUsername_notFound() {
        UUID id = UuidV7Utils.generateUuid();
        when(repository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(userService.updateUsername(id, "newname"))
            .verifyError(NotFoundException.class);
    }

    @Test
    void updatePassword_success() {
        String username = "testuser";
        String oldPassword = "oldpass";
        final String newPassword = "newpass";
        UUID userId = UuidV7Utils.generateUuid();

        UserEntity entity = UserEntity.builder()
            .username(username)
            .password("encodedoldpass")
            .build();
        entity.setId(userId);

        when(repository.findOne(any(Example.class)))
            .thenReturn(Mono.just(entity));
        when(passwordEncoder.matches(eq(oldPassword), any(String.class)))
            .thenReturn(true);
        when(passwordEncoder.matches(eq(newPassword), any(String.class)))
            .thenReturn(false);
        when(passwordEncoder.encode(newPassword)).thenReturn("encodednewpass");
        when(repository.save(any(UserEntity.class)))
            .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(userService.updatePassword(username, oldPassword, newPassword))
            .verifyComplete();

        verify(passwordEncoder).encode(newPassword);
        verify(repository).save(any(UserEntity.class));
    }

    @Test
    void updatePassword_oldPasswordNotMatching() {
        String username = "testuser";
        UUID userId = UuidV7Utils.generateUuid();

        UserEntity entity = UserEntity.builder()
            .username(username)
            .password("encodedoldpass")
            .build();
        entity.setId(userId);

        when(repository.findOne(any(Example.class)))
            .thenReturn(Mono.just(entity));
        when(passwordEncoder.matches(any(String.class), any(String.class)))
            .thenReturn(false);

        StepVerifier.create(userService.updatePassword(username, "wrongold", "newpass"))
            .verifyError(PasswordNotMatchingException.class);
    }

    @Test
    void update_success() {
        UUID userId = UuidV7Utils.generateUuid();
        UserEntity entity = UserEntity.builder()
            .username("testuser")
            .password("password")
            .build();
        entity.setId(userId);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("testuser");
        request.setNickname("New Nickname");
        request.setAvatar("http://new-avatar.jpg");

        when(repository.findByUsernameAndDeleteStatus("testuser", false))
            .thenReturn(Mono.just(entity));
        when(repository.save(any(UserEntity.class)))
            .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(userService.update(request))
            .assertNext(user -> {
                assertThat(user.entity().getNickname()).isEqualTo("New Nickname");
                assertThat(user.entity().getAvatar()).isEqualTo("http://new-avatar.jpg");
            })
            .verifyComplete();

        verify(applicationEventPublisher).publishEvent(any(UserAvatarUpdateEvent.class));
    }

    @Test
    void update_userNotFound() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("nonexistent");

        when(repository.findByUsernameAndDeleteStatus("nonexistent", false))
            .thenReturn(Mono.empty());

        StepVerifier.create(userService.update(request))
            .verifyError(NotFoundException.class);
    }

    @Test
    void changeRole_returnsEmpty() {
        StepVerifier.create(userService.changeRole("user", UuidV7Utils.generateUuid()))
            .verifyComplete();
    }

    @Test
    void changeRole_nullUsername_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> userService.changeRole(null, UuidV7Utils.generateUuid()));
    }

    @Test
    void sendVerificationCode_unsupportedEmailType() {
        UUID userId = UuidV7Utils.generateUuid();

        StepVerifier.create(userService.sendVerificationCode(userId,
                run.ikaros.api.core.user.enums.VerificationCodeType.EMAIL))
            .verifyError(UnsupportedOperationException.class);
    }

    @Test
    void sendVerificationCode_unsupportedPhoneMsgType() {
        UUID userId = UuidV7Utils.generateUuid();

        StepVerifier.create(userService.sendVerificationCode(userId,
                run.ikaros.api.core.user.enums.VerificationCodeType.PHONE_MSG))
            .verifyError(UnsupportedOperationException.class);
    }

    @Test
    void sendVerificationCode_nullUserId_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> userService.sendVerificationCode(null,
                run.ikaros.api.core.user.enums.VerificationCodeType.EMAIL));
    }
}
