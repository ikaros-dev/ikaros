package run.ikaros.storage;

import run.ikaros.storage.api.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

class AbstractS3StorageObjectProviderTest {
    @Test
    void cdnSigningKeepsConfiguredHostAndOmitsBucketFromPath() throws Exception {
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
                URI.create("https://origin.example"))
            .block().url();

        assertThat(URI.create(url).getHost()).isEqualTo("origin.example");
        assertThat(URI.create(url).getPath()).isEqualTo("/attachments/file.webp");
        assertThat(URI.create(url).getRawQuery()).contains("X-Amz-Signature=");
    }

    @Test
    void cdnSigningPrefixesBucketWhenEndpointHasNoHttpScheme() throws Exception {
        GenericS3StorageObjectProvider provider = new GenericS3StorageObjectProvider();
        StorageCredentialResolver credentials = mock(StorageCredentialResolver.class);
        when(credentials.resolve("secret://media"))
            .thenReturn(Mono.just(StaticCredentialsProvider.create(AwsBasicCredentials.create("access", "secret"))));
        Field field = AbstractS3StorageObjectProvider.class.getDeclaredField("credentialResolver");
        field.setAccessible(true);
        field.set(provider, credentials);

        StorageProvider storage = new StorageProvider(UUID.randomUUID(), "media", "S3", StorageTier.HOT,
            StorageProviderStatus.ENABLED, "secret://media", Map.of("bucket", "media", "endpoint", "oss.example.com"),
            Instant.now(), Instant.now());

        String url = provider.createReadIntent(storage, "attachments/file.webp", URI.create("//oss.example.com"))
            .block().url();

        assertThat(URI.create(url).getHost()).isEqualTo("media.oss.example.com");
        assertThat(URI.create(url).getPath()).isEqualTo("/attachments/file.webp");
    }
}
