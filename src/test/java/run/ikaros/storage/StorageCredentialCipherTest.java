package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import run.ikaros.common.ConflictException;

class StorageCredentialCipherTest {
    private static final String KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    @Test
    void encryptsAndDecryptsCredentialWithConfiguredKey() {
        StorageCredentialCipher cipher = new StorageCredentialCipher(KEY);

        String encrypted = cipher.encrypt("access-key");

        assertThat(encrypted).isNotEqualTo("access-key");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("access-key");
    }

    @Test
    void rejectsMissingOrInvalidConfiguredKey() {
        StorageCredentialCipher cipher = new StorageCredentialCipher("");

        assertThatThrownBy(() -> cipher.encrypt("access-key"))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("32 字节 Base64");
    }
}
