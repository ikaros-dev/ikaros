package run.ikaros.storage;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

/** 将 Provider 的 secret:// 引用解析为 SDK 凭据，不把凭据写入数据库。 */
public interface StorageCredentialResolver {
    Mono<AwsCredentialsProvider> resolve(String secretReference);
}
