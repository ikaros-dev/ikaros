package run.ikaros.server.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.api.core.authority.Authority;
import run.ikaros.api.store.enums.AuthorityType;

class IkarosGrantedAuthorityTest {

    @Test
    void getAuthorityReturnsCorrectFormat() {
        Authority authority = Authority.builder()
            .id(UUID.randomUUID())
            .type(AuthorityType.API)
            .target("/api/v1/users")
            .authority("read")
            .build();

        IkarosGrantedAuthority grantedAuthority = new IkarosGrantedAuthority(authority);
        String result = grantedAuthority.getAuthority();

        String expected = AuthorityType.API.name()
            + SecurityConst.AUTHORITY_DIVIDE + "/api/v1/users"
            + SecurityConst.AUTHORITY_DIVIDE + "read";
        assertEquals(expected, result);
    }

    @Test
    void getAuthorityWithMenuType() {
        Authority authority = Authority.builder()
            .id(UUID.randomUUID())
            .type(AuthorityType.MENU)
            .target("dashboard")
            .authority("view")
            .build();

        IkarosGrantedAuthority grantedAuthority = new IkarosGrantedAuthority(authority);
        String result = grantedAuthority.getAuthority();

        String expected = AuthorityType.MENU.name()
            + SecurityConst.AUTHORITY_DIVIDE + "dashboard"
            + SecurityConst.AUTHORITY_DIVIDE + "view";
        assertEquals(expected, result);
    }

    @Test
    void getAuthorityWithAllType() {
        Authority authority = Authority.builder()
            .id(UUID.randomUUID())
            .type(AuthorityType.ALL)
            .target("*")
            .authority("*")
            .build();

        IkarosGrantedAuthority grantedAuthority = new IkarosGrantedAuthority(authority);
        String result = grantedAuthority.getAuthority();

        String expected = AuthorityType.ALL.name()
            + SecurityConst.AUTHORITY_DIVIDE + "*"
            + SecurityConst.AUTHORITY_DIVIDE + "*";
        assertEquals(expected, result);
    }

    @Test
    void getAuthorityWithUrlType() {
        Authority authority = Authority.builder()
            .id(UUID.randomUUID())
            .type(AuthorityType.URL)
            .target("http://example.com")
            .authority("access")
            .build();

        IkarosGrantedAuthority grantedAuthority = new IkarosGrantedAuthority(authority);
        String result = grantedAuthority.getAuthority();

        String expected = AuthorityType.URL.name()
            + SecurityConst.AUTHORITY_DIVIDE + "http://example.com"
            + SecurityConst.AUTHORITY_DIVIDE + "access";
        assertEquals(expected, result);
    }

    @Test
    void authorityGetterReturnsWrappedAuthority() {
        Authority authority = Authority.builder()
            .id(UUID.randomUUID())
            .type(AuthorityType.APIS)
            .target("api-target")
            .authority("write")
            .build();

        IkarosGrantedAuthority grantedAuthority = new IkarosGrantedAuthority(authority);
        assertNotNull(grantedAuthority.authority());
        assertEquals(authority, grantedAuthority.authority());
        assertEquals(AuthorityType.APIS, grantedAuthority.authority().getType());
        assertEquals("api-target", grantedAuthority.authority().getTarget());
        assertEquals("write", grantedAuthority.authority().getAuthority());
    }

    @Test
    void formatContainsThreeSegments() {
        Authority authority = Authority.builder()
            .id(UUID.randomUUID())
            .type(AuthorityType.OTHERS)
            .target("some-target")
            .authority("some-authority")
            .build();

        IkarosGrantedAuthority grantedAuthority = new IkarosGrantedAuthority(authority);
        String result = grantedAuthority.getAuthority();

        String[] segments = result.split(SecurityConst.AUTHORITY_DIVIDE, -1);
        assertEquals(3, segments.length, "Authority string should contain exactly 3 segments");
        assertEquals("OTHERS", segments[0]);
        assertEquals("some-target", segments[1]);
        assertEquals("some-authority", segments[2]);
    }
}
