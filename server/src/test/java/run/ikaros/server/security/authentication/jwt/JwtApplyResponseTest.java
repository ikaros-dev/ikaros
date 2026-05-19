package run.ikaros.server.security.authentication.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class JwtApplyResponseTest {

    @Test
    void builderPattern() {
        JwtApplyResponse response = JwtApplyResponse.builder()
            .username("testUser")
            .accessToken("access-token-value")
            .refreshToken("refresh-token-value")
            .build();

        assertNotNull(response);
        assertEquals("testUser", response.getUsername());
        assertEquals("access-token-value", response.getAccessToken());
        assertEquals("refresh-token-value", response.getRefreshToken());
    }

    @Test
    void gettersAndSetters() {
        JwtApplyResponse response = new JwtApplyResponse();
        response.setUsername("user2");
        response.setAccessToken("new-access-token");
        response.setRefreshToken("new-refresh-token");

        assertEquals("user2", response.getUsername());
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
    }

    @Test
    void chainSetters() {
        JwtApplyResponse response = new JwtApplyResponse();
        JwtApplyResponse returned = response
            .setUsername("chainUser")
            .setAccessToken("chain-access")
            .setRefreshToken("chain-refresh");

        assertSame(response, returned);
        assertEquals("chainUser", response.getUsername());
        assertEquals("chain-access", response.getAccessToken());
        assertEquals("chain-refresh", response.getRefreshToken());
    }

    @Test
    void noArgsConstructor() {
        JwtApplyResponse response = new JwtApplyResponse();
        assertNotNull(response);
        assertEquals(null, response.getUsername());
        assertEquals(null, response.getAccessToken());
        assertEquals(null, response.getRefreshToken());
    }

    @Test
    void allArgsConstructor() {
        JwtApplyResponse response = new JwtApplyResponse(
            "allArgUser",
            "allArgAccess",
            "allArgRefresh"
        );

        assertEquals("allArgUser", response.getUsername());
        assertEquals("allArgAccess", response.getAccessToken());
        assertEquals("allArgRefresh", response.getRefreshToken());
    }

    @Test
    void builderWithNullValues() {
        JwtApplyResponse response = JwtApplyResponse.builder()
            .username(null)
            .accessToken(null)
            .refreshToken(null)
            .build();

        assertNotNull(response);
        assertEquals(null, response.getUsername());
        assertEquals(null, response.getAccessToken());
        assertEquals(null, response.getRefreshToken());
    }
}
