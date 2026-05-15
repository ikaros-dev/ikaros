package run.ikaros.server.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.api.store.entity.Authority;

class IkarosGrantedAuthorityTest {

    @Test
    void getAuthority_shouldFormatWithTypeTargetAuthority() {
        Authority authority = new Authority();
        authority.setType("ALL");
        authority.setTarget("*");
        authority.setAuthority("*");

        IkarosGrantedAuthority granted = new IkarosGrantedAuthority(authority);

        String expected = "ALL" + SecurityConst.AUTHORITY_DIVIDE
            + "*" + SecurityConst.AUTHORITY_DIVIDE
            + "*";
        assertEquals(expected, granted.getAuthority());
    }

    @Test
    void getAuthority_apiType_shouldFormatCorrectly() {
        Authority authority = new Authority();
        authority.setType("API");
        authority.setTarget("/api/v1/user/**");
        authority.setAuthority("HTTP_GET");

        IkarosGrantedAuthority granted = new IkarosGrantedAuthority(authority);

        String expected = "API" + SecurityConst.AUTHORITY_DIVIDE
            + "/api/v1/user/**" + SecurityConst.AUTHORITY_DIVIDE
            + "HTTP_GET";
        assertEquals(expected, granted.getAuthority());
    }

    @Test
    void getAuthority_shouldNotBeNull() {
        Authority authority = new Authority();
        authority.setType("MENU");
        authority.setTarget("/dashboard/**");
        authority.setAuthority("*");

        IkarosGrantedAuthority granted = new IkarosGrantedAuthority(authority);

        assertNotNull(granted.getAuthority());
    }

    @Test
    void authority_shouldReturnAuthorityDivideConstant() {
        assertEquals("&&&&&&", SecurityConst.AUTHORITY_DIVIDE);
    }

    @Test
    void getAuthority_singleAuthorityField() {
        Authority authority = new Authority();
        authority.setType("ALL");
        authority.setTarget("*");
        authority.setAuthority("*");

        IkarosGrantedAuthority granted = new IkarosGrantedAuthority(authority);
        String auth = granted.getAuthority();

        // Should contain exactly 2 dividers
        String[] parts = auth.split(SecurityConst.AUTHORITY_DIVIDE);
        assertEquals(3, parts.length);
        assertEquals("ALL", parts[0]);
        assertEquals("*", parts[1]);
        assertEquals("*", parts[2]);
    }
}
