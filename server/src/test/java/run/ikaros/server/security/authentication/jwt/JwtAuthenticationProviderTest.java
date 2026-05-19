package run.ikaros.server.security.authentication.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.test.StepVerifier;
import run.ikaros.server.security.SecurityProperties;

class JwtAuthenticationProviderTest {

    private JwtAuthenticationProvider jwtAuthenticationProvider;
    private SecurityProperties securityProperties;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        securityProperties.getExpiry().setAccessTokenDay(3);
        securityProperties.getExpiry().setRefreshTokenMonth(3);
        jwtAuthenticationProvider = new JwtAuthenticationProvider(securityProperties);
    }

    @Test
    void generateJwtResp_success() {
        UserDetails userDetails = User.withUsername("testuser")
            .password("pass")
            .roles("USER")
            .build();

        StepVerifier.create(jwtAuthenticationProvider.generateJwtResp(userDetails))
            .assertNext(response -> {
                assertThat(response.getUsername()).isEqualTo("testuser");
                assertThat(response.getAccessToken()).isNotBlank();
                assertThat(response.getRefreshToken()).isNotBlank();
                assertThat(response.getAccessToken()).isNotEqualTo(response.getRefreshToken());
            })
            .verifyComplete();
    }

    @Test
    void validateToken_valid() {
        UserDetails userDetails = User.withUsername("testuser")
            .password("pass")
            .roles("USER")
            .build();

        // First generate tokens to set expiry fields and obtain a valid token
        JwtApplyResponse response = jwtAuthenticationProvider.generateJwtResp(userDetails).block();

        assertThat(jwtAuthenticationProvider.validateToken(response.getAccessToken())).isTrue();
        assertThat(jwtAuthenticationProvider.validateToken(response.getRefreshToken())).isTrue();
    }

    @Test
    void validateToken_invalid() {
        assertThat(jwtAuthenticationProvider.validateToken("invalid-token")).isFalse();
    }

    @Test
    void extractUsername_success() {
        UserDetails userDetails = User.withUsername("testuser")
            .password("pass")
            .roles("USER")
            .build();

        JwtApplyResponse response = jwtAuthenticationProvider.generateJwtResp(userDetails).block();

        String username = jwtAuthenticationProvider.extractUsername(response.getAccessToken());
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void refreshToken_valid() {
        UserDetails userDetails = User.withUsername("testuser")
            .password("pass")
            .roles("USER")
            .build();

        // Generate tokens first to set accessTokenExpiry and refreshTokenExpiry
        JwtApplyResponse response = jwtAuthenticationProvider.generateJwtResp(userDetails).block();

        StepVerifier.create(jwtAuthenticationProvider.refreshToken(response.getRefreshToken()))
            .assertNext(newAccessToken -> {
                assertThat(newAccessToken).isNotBlank();
                // Verify the new token is valid and contains the correct username
                assertThat(jwtAuthenticationProvider.validateToken(newAccessToken)).isTrue();
                assertThat(jwtAuthenticationProvider.extractUsername(newAccessToken))
                    .isEqualTo("testuser");
            })
            .verifyComplete();
    }

    @Test
    void refreshToken_invalid() {
        StepVerifier.create(jwtAuthenticationProvider.refreshToken("invalid-token"))
            .expectError()
            .verify();
    }
}
