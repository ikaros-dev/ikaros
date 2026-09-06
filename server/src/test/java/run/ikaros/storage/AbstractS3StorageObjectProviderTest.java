package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

class AbstractS3StorageObjectProviderTest {
    @Test
    void cdnSigningKeepsConfiguredHostWhenItAlreadyContainsBucketPrefix() throws Exception {
        GenericS3StorageObjectProvider provider = new GenericS3StorageObjectProvider();
        StorageCredentialResolver credentials = mock(StorageCredentialResolver.class);
        when(credentials.resolve("secret://media"))
            .thenReturn(Mono.just(StaticCredentialsProvider.create(AwsBasicCredentials.create("access", "secret"))));
        Field field = AbstractS3StorageObjectProvider.class.getDeclaredField("credentialResolver");
        field.setAccessible(true);
        field.set(provider, credentials);

        StorageProvider storage = new StorageProvider(UUID.randomUUID(), "media", "S3", StorageTier.HOT,
            StorageProviderStatus.ENABLED, "secret://media", Map.of("bucket", "media", "endpoint", "https://origin.example"),
            Instant.now(), Instant.now());

        String url = provider.createReadIntent(storage, "attachments/file.webp",
                java.net.URI.create("https://media.origin.example"))
            .block().url();

        assertThat(java.net.URI.create(url).getHost()).isEqualTo("media.origin.example");
    }
}
