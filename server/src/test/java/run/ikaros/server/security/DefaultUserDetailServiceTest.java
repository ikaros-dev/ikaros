package run.ikaros.server.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import run.ikaros.api.store.entity.Authority;
import run.ikaros.api.store.entity.Role;
import run.ikaros.api.store.entity.RoleAuthority;
import run.ikaros.api.store.entity.User;
import run.ikaros.api.store.entity.UserRole;
import run.ikaros.server.common.TestEntityIds;
import run.ikaros.server.security.exception.RoleNotFoundException;
import run.ikaros.server.security.exception.UserHasNotRoleException;
import run.ikaros.server.store.mapper.AuthorityMapper;
import run.ikaros.server.store.mapper.RoleAuthorityMapper;
import run.ikaros.server.store.mapper.RoleMapper;
import run.ikaros.server.store.mapper.UserMapper;
import run.ikaros.server.store.mapper.UserRoleMapper;

class DefaultUserDetailServiceTest {

    private UserMapper userMapper;
    private UserRoleMapper userRoleMapper;
    private RoleMapper roleMapper;
    private RoleAuthorityMapper roleAuthorityMapper;
    private AuthorityMapper authorityMapper;
    private DefaultUserDetailService service;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        userRoleMapper = mock(UserRoleMapper.class);
        roleMapper = mock(RoleMapper.class);
        roleAuthorityMapper = mock(RoleAuthorityMapper.class);
        authorityMapper = mock(AuthorityMapper.class);
        service = new DefaultUserDetailService(
            userMapper, userRoleMapper, roleMapper, roleAuthorityMapper, authorityMapper);
    }

    @Test
    void loadUserByUsername_validUser_shouldReturnSecurityUser() {
        User user = new User();
        user.setId(TestEntityIds.USER_1);
        user.setUsername("testuser");
        user.setPassword("encoded_pass");
        user.setEnable(true);
        user.setDeleteStatus(false);
        when(userMapper.findByUsernameAndEnableAndDeleteStatus("testuser", true, false))
            .thenReturn(user);

        UserRole userRole = new UserRole();
        userRole.setUserId(TestEntityIds.USER_1);
        userRole.setRoleId(TestEntityIds.ROLE_10);
        when(userRoleMapper.findByUserId(TestEntityIds.USER_1)).thenReturn(userRole);

        Role role = new Role();
        role.setId(TestEntityIds.ROLE_10);
        role.setName("MASTER");
        when(roleMapper.selectById(TestEntityIds.ROLE_10)).thenReturn(role);

        RoleAuthority ra = new RoleAuthority();
        ra.setId(TestEntityIds.AUTH_100);
        ra.setAuthorityId(TestEntityIds.AUTH_200);
        when(roleAuthorityMapper.findAllByRoleId(TestEntityIds.ROLE_10))
            .thenReturn(List.of(ra));

        Authority authority = new Authority();
        authority.setId(TestEntityIds.AUTH_200);
        authority.setType("ALL");
        authority.setTarget("*");
        authority.setAuthority("*");
        when(authorityMapper.selectById(TestEntityIds.AUTH_200)).thenReturn(authority);

        UserDetails result = service.loadUserByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encoded_pass", result.getPassword());
        assertEquals(1, result.getAuthorities().size());
    }

    @Test
    void loadUserByUsername_userNotFound_shouldThrowUsernameNotFoundException() {
        when(userMapper.findByUsernameAndEnableAndDeleteStatus("unknown", true, false))
            .thenReturn(null);

        assertThrows(UsernameNotFoundException.class,
            () -> service.loadUserByUsername("unknown"));
    }

    @Test
    void loadUserByUsername_userHasNoRole_shouldThrowUserHasNotRoleException() {
        User user = new User();
        user.setId(TestEntityIds.USER_1);
        user.setUsername("orphanuser");
        user.setEnable(true);
        when(userMapper.findByUsernameAndEnableAndDeleteStatus("orphanuser", true, false))
            .thenReturn(user);
        when(userRoleMapper.findByUserId(TestEntityIds.USER_1)).thenReturn(null);

        assertThrows(UserHasNotRoleException.class,
            () -> service.loadUserByUsername("orphanuser"));
    }

    @Test
    void loadUserByUsername_roleNotFound_shouldThrowRoleNotFoundException() {
        User user = new User();
        user.setId(TestEntityIds.USER_1);
        user.setUsername("testuser");
        user.setEnable(true);
        when(userMapper.findByUsernameAndEnableAndDeleteStatus("testuser", true, false))
            .thenReturn(user);

        UserRole userRole = new UserRole();
        userRole.setUserId(TestEntityIds.USER_1);
        userRole.setRoleId(TestEntityIds.ROLE_99);
        when(userRoleMapper.findByUserId(TestEntityIds.USER_1)).thenReturn(userRole);
        when(roleMapper.selectById(TestEntityIds.ROLE_99)).thenReturn(null);

        assertThrows(RoleNotFoundException.class,
            () -> service.loadUserByUsername("testuser"));
    }

    @Test
    void loadUserByUsername_emptyUsername_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.loadUserByUsername(""));
    }

    @Test
    void loadUserByUsername_nullUsername_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.loadUserByUsername(null));
    }

    @Test
    void loadUserByUsername_multipleAuthorities_shouldLoadAll() {
        User user = new User();
        user.setId(TestEntityIds.USER_1);
        user.setUsername("multiuser");
        user.setPassword("pass");
        user.setEnable(true);
        when(userMapper.findByUsernameAndEnableAndDeleteStatus("multiuser", true, false))
            .thenReturn(user);

        UserRole userRole = new UserRole();
        userRole.setUserId(TestEntityIds.USER_1);
        userRole.setRoleId(TestEntityIds.ROLE_10);
        when(userRoleMapper.findByUserId(TestEntityIds.USER_1)).thenReturn(userRole);

        Role role = new Role();
        role.setId(TestEntityIds.ROLE_10);
        role.setName("ADMIN");
        when(roleMapper.selectById(TestEntityIds.ROLE_10)).thenReturn(role);

        RoleAuthority ra1 = new RoleAuthority();
        ra1.setId(TestEntityIds.ROLE_AUTH_1);
        ra1.setAuthorityId(TestEntityIds.AUTH_101);
        RoleAuthority ra2 = new RoleAuthority();
        ra2.setId(TestEntityIds.ROLE_AUTH_2);
        ra2.setAuthorityId(TestEntityIds.AUTH_102);
        when(roleAuthorityMapper.findAllByRoleId(TestEntityIds.ROLE_10))
            .thenReturn(List.of(ra1, ra2));

        Authority a1 = new Authority();
        a1.setId(TestEntityIds.AUTH_101);
        a1.setType("API");
        a1.setTarget("/api/v1/user/**");
        a1.setAuthority("HTTP_GET");
        Authority a2 = new Authority();
        a2.setId(TestEntityIds.AUTH_102);
        a2.setType("API");
        a2.setTarget("/api/v1/subject/**");
        a2.setAuthority("*");
        when(authorityMapper.selectById(TestEntityIds.AUTH_101)).thenReturn(a1);
        when(authorityMapper.selectById(TestEntityIds.AUTH_102)).thenReturn(a2);

        UserDetails result = service.loadUserByUsername("multiuser");

        assertNotNull(result);
        assertEquals(2, result.getAuthorities().size());
    }
}
