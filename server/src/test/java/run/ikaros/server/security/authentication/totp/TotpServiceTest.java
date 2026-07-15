package run.ikaros.server.security.authentication.totp;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TotpServiceTest {
    private TotpService totpService;

    @BeforeEach
    void setUp() {
        totpService = new TotpService();
    }

    @Test
    void generateSecret() {
        String secret = totpService.generateSecret();
        assertThat(secret).isNotBlank();
        // Base32 encoded, should not contain '='
        assertThat(secret).doesNotContain("=");
        Base32 base32 = new Base32();
        byte[] decoded = base32.decode(secret);
        assertThat(decoded).hasSize(20);
    }

    @Test
    void generateOtpAuthUri() {
        String secret = totpService.generateSecret();
        String uri = totpService.generateOtpAuthUri("testuser", secret);

        assertThat(uri).startsWith("otpauth://totp/Ikaros:testuser");
        assertThat(uri).contains("secret=" + secret);
        assertThat(uri).contains("issuer=Ikaros");
        assertThat(uri).contains("algorithm=SHA1");
        assertThat(uri).contains("digits=6");
        assertThat(uri).contains("period=30");
    }

    @Test
    void generateOtpAuthUri_withBlankUsername_throwsException() {
        String secret = totpService.generateSecret();
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> totpService.generateOtpAuthUri("", secret)
        );
    }

    @Test
    void generateOtpAuthUri_withBlankSecret_throwsException() {
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> totpService.generateOtpAuthUri("testuser", "")
        );
    }

    @Test
    void validateCode_withWrongCode_returnsFalse() {
        String secret = totpService.generateSecret();
        boolean result = totpService.validateCode(secret, "000000");
        assertThat(result).isFalse();
    }

    @Test
    void validateCode_withInvalidLength_returnsFalse() {
        String secret = totpService.generateSecret();
        assertThat(totpService.validateCode(secret, "12345")).isFalse();
        assertThat(totpService.validateCode(secret, "1234567")).isFalse();
    }

    @Test
    void validateCode_withNonDigit_returnsFalse() {
        String secret = totpService.generateSecret();
        assertThat(totpService.validateCode(secret, "abc123")).isFalse();
    }

    @Test
    void validateCode_withBlankSecret_throwsException() {
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> totpService.validateCode("", "123456")
        );
    }

    @Test
    void validateCode_withBlankCode_throwsException() {
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> totpService.validateCode(totpService.generateSecret(), "")
        );
    }
}
