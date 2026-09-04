package run.ikaros.storage;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.Base64;
import java.util.HexFormat;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/** S3 API implementation shared by cloud vendors exposing S3-compatible APIs. */
abstract class AbstractS3StorageObjectProvider implements StorageObjectProvider {
    private static final Duration URL_TTL = Duration.ofMinutes(15);
    @Value("${ikaros.storage.upload-url-ttl:PT15M}")
    private Duration timeout = URL_TTL;
    @org.springframework.beans.factory.annotation.Autowired
    private StorageCredentialResolver credentialResolver;

    @Override
    public Mono<StorageUploadIntent> createUploadIntent(StorageProvider provider, StorageUploadRequest request) {
        return credentialResolver.resolve(provider.secretReference()).flatMap(credentials -> Mono.fromCallable(() -> {
            S3Settings settings = S3Settings.from(provider);
            try (S3Presigner presigner = S3Presigner.builder().region(Region.of(settings.region()))
                .endpointOverride(settings.endpoint()).credentialsProvider(credentials).build()) {
                PutObjectPresignRequest presign = PutObjectPresignRequest.builder().signatureDuration(timeout)
                    .putObjectRequest(builder -> builder.bucket(settings.bucket()).key(request.objectKey())
                        .contentLength(request.sizeBytes()).contentType(request.mediaType())
                        .checksumSHA256(checksumHeader(request.sha256())).build()).build();
                String url = presigner.presignPutObject(presign).url().toString();
                return new StorageUploadIntent("PUT", url, request.objectKey(), Instant.now().plus(timeout));
            }
        })).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<StorageReadIntent> createReadIntent(StorageProvider provider, String objectKey) {
        return credentialResolver.resolve(provider.secretReference()).flatMap(credentials -> Mono.fromCallable(() -> {
            S3Settings settings = S3Settings.from(provider);
            try (S3Presigner presigner = S3Presigner.builder().region(Region.of(settings.region()))
                .endpointOverride(settings.endpoint()).credentialsProvider(credentials).build()) {
                GetObjectPresignRequest presign = GetObjectPresignRequest.builder().signatureDuration(timeout)
                    .getObjectRequest(GetObjectRequest.builder().bucket(settings.bucket()).key(objectKey).build()).build();
                return new StorageReadIntent("GET", presigner.presignGetObject(presign).url().toString(), Instant.now().plus(timeout));
            }
        })).subscribeOn(Schedulers.boundedElastic());
    }

    private String checksumHeader(String sha256) {
        return sha256 == null ? null : Base64.getEncoder().encodeToString(HexFormat.of().parseHex(sha256));
    }

    @Override
    public Mono<StorageObjectMetadata> verify(StorageProvider provider, String objectKey) {
        return credentialResolver.resolve(provider.secretReference()).flatMap(credentials -> Mono.fromCallable(() -> {
            S3Settings settings = S3Settings.from(provider);
            return withClient(settings, credentials, client -> {
                var object = client.headObject(HeadObjectRequest.builder().bucket(settings.bucket()).key(objectKey)
                    .build());
                return new StorageObjectMetadata(objectKey, object.contentLength(), object.contentType(), object.eTag(),
                    object.checksumSHA256());
            });
        })).subscribeOn(Schedulers.boundedElastic());
    }

    private <T> T withClient(S3Settings settings, software.amazon.awssdk.auth.credentials.AwsCredentialsProvider credentials,
                             java.util.function.Function<S3Client, T> action) {
        try (S3Client client = S3Client.builder().region(Region.of(settings.region()))
            .endpointOverride(settings.endpoint()).credentialsProvider(credentials).build()) {
            return action.apply(client);
        }
    }

    record S3Settings(String bucket, String region, URI endpoint) {
        static S3Settings from(StorageProvider provider) {
            Map<String, Object> metadata = provider.metadata();
            String bucket = required(metadata, "bucket");
            String region = String.valueOf(metadata.getOrDefault("region", "us-east-1"));
            String endpointText = required(metadata, "endpoint");
            return new S3Settings(bucket, region, URI.create(endpointText));
        }

        private static String required(Map<String, Object> metadata, String key) {
            return Optional.ofNullable(metadata.get(key)).map(Object::toString).filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("S3 Provider metadata 缺少 " + key));
        }
    }
}
