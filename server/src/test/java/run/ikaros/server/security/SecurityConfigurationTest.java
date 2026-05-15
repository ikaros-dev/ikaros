package run.ikaros.server.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import run.ikaros.server.store.mapper.AuthorityMapper;
import run.ikaros.server.store.mapper.RoleAuthorityMapper;
import run.ikaros.server.store.mapper.RoleMapper;
import run.ikaros.server.store.mapper.UserMapper;
import run.ikaros.server.store.mapper.UserRoleMapper;

class SecurityConfigurationTest {

    @Test
    void passwordEncoder_shouldReturnDelegatingEncoder() {
        SecurityConfiguration config = new SecurityConfiguration();

        PasswordEncoder encoder = config.passwordEncoder();

        assertNotNull(encoder);
        // DelegatingPasswordEncoder delegates to the stored encoder
        String raw = "testpass";
        String encoded = encoder.encode(raw);
        assertNotNull(encoded);
        assertTrue(encoder.matches(raw, encoded));
    }

    @Test
    void userDetailsService_shouldReturnDefaultUserDetailService() {
        SecurityConfiguration config = new SecurityConfiguration();
        UserMapper userMapper = mock(UserMapper.class);
        UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);
        RoleAuthorityMapper roleAuthorityMapper = mock(RoleAuthorityMapper.class);
        AuthorityMapper authorityMapper = mock(AuthorityMapper.class);

        UserDetailsService service = config.userDetailsService(
            userMapper, userRoleMapper, roleMapper,
            roleAuthorityMapper, authorityMapper);

        assertNotNull(service);
        assertTrue(service instanceof DefaultUserDetailService);
    }
}
