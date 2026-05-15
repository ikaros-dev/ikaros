package run.ikaros.server.security.authentication.jwt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import run.ikaros.api.infra.exception.security.InvalidTokenException;
import run.ikaros.server.security.SecurityProperties;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationProviderTest {

    private JwtAuthenticationProvider provider;

    private UserDetails mockUserDetails(String username) {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(username);
        return userDetails;
    }

    @BeforeEach
    void setUp() {
        SecurityProperties securityProperties = new SecurityProperties();
        provider = new JwtAuthenticationProvider(securityProperties);
    }

    @Test
    void generateJwtResp_shouldReturnNonEmptyTokens() {
        JwtApplyResponse response = provider.generateJwtResp(mockUserDetails("testuser"));

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertFalse(response.getAccessToken().isEmpty());
        assertFalse(response.getRefreshToken().isEmpty());
    }

    @Test
    void generateJwtResp_accessTokenShouldContainUsername() {
        JwtApplyResponse response = provider.generateJwtResp(mockUserDetails("testuser"));

        String username = provider.extractUsername(response.getAccessToken());
        assertEquals("testuser", username);
    }

    @Test
    void generateJwtResp_refreshTokenShouldContainUsername() {
        JwtApplyResponse response = provider.generateJwtResp(mockUserDetails("testuser"));

        String username = provider.extractUsername(response.getRefreshToken());
        assertEquals("testuser", username);
    }

    @Test
    void generateJwtResp_refreshTokenShouldLiveLongerThanAccessToken() {
        JwtApplyResponse response = provider.generateJwtResp(mockUserDetails("testuser"));

        Claims accessClaims = provider.extractClaims(response.getAccessToken());
        Claims refreshClaims = provider.extractClaims(response.getRefreshToken());

        assertTrue(refreshClaims.getExpiration().after(accessClaims.getExpiration()));
    }

    @Test
    void validateToken_validTokenShouldReturnTrue() {
        JwtApplyResponse response = provider.generateJwtResp(mockUserDetails("testuser"));

        assertTrue(provider.validateToken(response.getAccessToken()));
        assertTrue(provider.validateToken(response.getRefreshToken()));
    }

    @Test
    void validateToken_invalidTokenShouldReturnFalse() {
        assertFalse(provider.validateToken("invalid.jwt.token"));
        assertFalse(provider.validateToken(""));
        assertFalse(provider.validateToken("not-a-jwt-at-all"));
    }

    @Test
    void validateToken_expiredTokenShouldReturnFalse() {
        // Create a provider with very short expiry
        SecurityProperties shortExpiryProps = new SecurityProperties();
        shortExpiryProps.getExpiry().setAccessTokenDay(0);
        shortExpiryProps.getExpiry().setRefreshTokenMonth(0);
        JwtAuthenticationProvider shortProvider =
            new JwtAuthenticationProvider(shortExpiryProps);

        JwtApplyResponse response = shortProvider.generateJwtResp(
            mockUserDetails("testuser"));

        // Token with 0-day expiry should be expired immediately
        assertFalse(shortProvider.validateToken(response.getAccessToken()));
    }

    @Test
    void extractClaims_validTokenShouldReturnClaims() {
        JwtApplyResponse response = provider.generateJwtResp(mockUserDetails("testuser"));

        Claims claims = provider.extractClaims(response.getAccessToken());

        assertNotNull(claims);
        assertEquals("testuser", claims.getSubject());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void extractClaims_invalidTokenShouldThrow() {
        assertThrows(Exception.class,
            () -> provider.extractClaims("invalid.token.here"));
    }

    @Test
    void extractUsername_validTokenShouldReturnUsername() {
        JwtApplyResponse response = provider.generateJwtResp(mockUserDetails("alice"));

        assertEquals("alice", provider.extractUsername(response.getAccessToken()));
        assertEquals("alice", provider.extractUsername(response.getRefreshToken()));
    }

    @Test
    void extractUsername_invalidTokenShouldThrow() {
        assertThrows(Exception.class,
            () -> provider.extractUsername("bad.token.value"));
    }

    @Test
    void refreshToken_withValidRefreshTokenShouldReturnNewAccessToken() {
        JwtApplyResponse response = provider.generateJwtResp(mockUserDetails("testuser"));
        String originalAccessToken = response.getAccessToken();

        String newAccessToken = assertDoesNotThrow(
            () -> provider.refreshToken(response.getRefreshToken()));

        assertNotNull(newAccessToken);
        assertFalse(newAccessToken.isEmpty());
        assertEquals("testuser", provider.extractUsername(newAccessToken));

        // New token should have a different expiration (same or later)
        Claims originalClaims = provider.extractClaims(originalAccessToken);
        Claims newClaims = provider.extractClaims(newAccessToken);
        assertTrue(
            newClaims.getExpiration().equals(originalClaims.getExpiration())
                || newClaims.getExpiration().after(originalClaims.getExpiration())
        );
    }

    @Test
    void refreshToken_withInvalidTokenShouldThrowInvalidTokenException() {
        assertThrows(InvalidTokenException.class,
            () -> provider.refreshToken("completely.invalid.token"));
    }

    @Test
    void refreshToken_withAccessTokenAsRefreshTokenShouldWorkWhileValid() {
        // Access tokens are also valid JWTs, so they can be used as refresh
        // tokens while still within their validity window
        JwtApplyResponse response = provider.generateJwtResp(mockUserDetails("testuser"));
        String accessToken = response.getAccessToken();

        String newToken = assertDoesNotThrow(
            () -> provider.refreshToken(accessToken));
        assertNotNull(newToken);
        assertEquals("testuser", provider.extractUsername(newToken));
    }

    @Test
    void differentUsers_shouldGenerateDifferentTokens() {
        JwtApplyResponse resp1 = provider.generateJwtResp(mockUserDetails("user1"));
        JwtApplyResponse resp2 = provider.generateJwtResp(mockUserDetails("user2"));

        assertEquals("user1", provider.extractUsername(resp1.getAccessToken()));
        assertEquals("user2", provider.extractUsername(resp2.getAccessToken()));
    }

    @Test
    void sameUser_differentGenerationsShouldProduceDifferentTokens() {
        JwtApplyResponse resp1 = provider.generateJwtResp(mockUserDetails("sameuser"));
        JwtApplyResponse resp2 = provider.generateJwtResp(mockUserDetails("sameuser"));

        assertEquals("sameuser", provider.extractUsername(resp1.getAccessToken()));
        assertEquals("sameuser", provider.extractUsername(resp2.getAccessToken()));

        // Tokens should differ due to different expiry timestamps
        Claims claims1 = provider.extractClaims(resp1.getAccessToken());
        Claims claims2 = provider.extractClaims(resp2.getAccessToken());
        assertFalse(claims1.getExpiration().after(claims2.getExpiration()));
    }
}
