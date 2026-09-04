package run.ikaros.storage;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import run.ikaros.common.ConflictException;

/** 默认解析器：支持 SDK 默认链和显式环境变量引用。 */
@Component
public class EnvironmentStorageCredentialResolver implements StorageCredentialResolver {
    private final StorageProviderRepository providers;
    private final StorageCredentialCipher cipher;

    public EnvironmentStorageCredentialResolver(StorageProviderRepository providers, StorageCredentialCipher cipher) { this.providers = providers; this.cipher = cipher; }
    @Override
    public Mono<AwsCredentialsProvider> resolve(String secretReference) {
        if ("secret://default".equalsIgnoreCase(secretReference)) {
            return Mono.just(DefaultCredentialsProvider.create());
        }
        if (secretReference != null && secretReference.startsWith("secret://provider/")) {
            String key = secretReference.substring("secret://provider/".length());
            return providers.findByProviderKey(key).switchIfEmpty(Mono.error(new ConflictException("Storage Provider 不存在: " + key)))
                .flatMap(provider -> { if (provider.accessKeyIdCiphertext() == null || provider.secretAccessKeyCiphertext() == null) return Mono.error(new ConflictException("Provider 未配置 AccessKey/SecretKey")); String access = cipher.decrypt(provider.accessKeyIdCiphertext()); String secret = cipher.decrypt(provider.secretAccessKeyCiphertext()); String session = cipher.decrypt(provider.sessionTokenCiphertext()); return Mono.just(session == null ? StaticCredentialsProvider.create(AwsBasicCredentials.create(access, secret)) : StaticCredentialsProvider.create(AwsSessionCredentials.create(access, secret, session))); });
        }
        if (secretReference == null || !secretReference.startsWith("secret://env/")) {
            return Mono.error(new ConflictException("不支持的 Storage Provider secret reference；请使用 secret://default 或 secret://env/NAME"));
        }
        String variable = secretReference.substring("secret://env/".length()).trim();
        String value = variable.isBlank() ? null : System.getenv(variable);
        if (value == null || value.isBlank()) return Mono.error(new ConflictException("Storage Provider 凭据环境变量不存在: " + variable));
        String[] parts = value.split(":", -1);
        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return Mono.error(new ConflictException("Storage Provider 凭据环境变量格式应为 accessKeyId:secretAccessKey[:sessionToken]"));
        }
        AwsCredentialsProvider provider = parts.length >= 3 && !parts[2].isBlank()
            ? StaticCredentialsProvider.create(AwsSessionCredentials.create(parts[0], parts[1], parts[2]))
            : StaticCredentialsProvider.create(AwsBasicCredentials.create(parts[0], parts[1]));
        return Mono.just(provider);
    }
}
