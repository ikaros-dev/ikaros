package run.ikaros.server.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.core.role.RoleService;
import run.ikaros.server.core.user.User;
import run.ikaros.server.core.user.UserService;
import run.ikaros.server.store.entity.UserEntity;
import run.ikaros.server.store.repository.UserRoleRepository;

class MasterInitializerTest {

    private SecurityProperties securityProperties;
    private UserService userService;
    private RoleService roleService;
    private UserRoleRepository userRoleRepository;
    private MasterInitializer masterInitializer;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        userService = mock(UserService.class);
        roleService = mock(RoleService.class);
        userRoleRepository = mock(UserRoleRepository.class);
        masterInitializer = new MasterInitializer(
            securityProperties, userService, roleService, userRoleRepository);
    }

    @Test
    void initialize_disabled_returnsEmpty() {
        // Given
        securityProperties.getInitializer().setDisabled(true);

        // When & Then
        StepVerifier.create(masterInitializer.initialize())
            .verifyComplete();

        verify(userService, never()).getUserByUsername(anyString());
    }

    @Test
    void initialize_existingMaster_skips() {
        // Given
        securityProperties.getInitializer().setDisabled(false);

        UserEntity userEntity = UserEntity.builder()
            .username("tomoki")
            .password("password")
            .nickname("test")
            .enable(true)
            .build();
        userEntity.setId(UuidV7Utils.generateUuid());

        when(userService.getUserByUsername("tomoki"))
            .thenReturn(Mono.just(new User(userEntity)));

        // When & Then
        StepVerifier.create(masterInitializer.initialize())
            .verifyComplete();

        verify(userService).getUserByUsername("tomoki");
        verify(roleService, never()).save(
            org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getPassword_withConfiguredPassword() throws Exception {
        // Given
        securityProperties.getInitializer().setMasterPassword("secret123");

        // When - use reflection to access private method
        Method getPasswordMethod = MasterInitializer.class.getDeclaredMethod("getPassword");
        getPasswordMethod.setAccessible(true);
        String password = (String) getPasswordMethod.invoke(masterInitializer);

        // Then
        assertEquals("secret123", password);
    }

    @Test
    void getPassword_generatesRandom() throws Exception {
        // Given
        securityProperties.getInitializer().setMasterPassword(null);

        // When
        Method getPasswordMethod = MasterInitializer.class.getDeclaredMethod("getPassword");
        getPasswordMethod.setAccessible(true);
        String password = (String) getPasswordMethod.invoke(masterInitializer);

        // Then
        assertNotNull(password);
        assertEquals(16, password.length());
        assertTrue(password.chars().allMatch(Character::isLetterOrDigit));
    }
}
