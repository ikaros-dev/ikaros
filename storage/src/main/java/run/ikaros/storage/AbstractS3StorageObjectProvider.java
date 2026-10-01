package run.ikaros.storage;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.Base64;
import java.util.HexFormat;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import run.ikaros.storage.api.StorageProviderProbeResult;
import run.ikaros.storage.api.StorageProviderProbeStatus;

/** S3 API implementation shared by cloud vendors exposing S3-compatible APIs. */
abstract class AbstractS3StorageObjectProvider implements StorageObjectProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractS3StorageObjectProvider.class);
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
        return createReadIntent(provider, objectKey, null);
    }

    @Override
    public Mono<StorageReadIntent> createReadIntent(StorageProvider provider, String objectKey, URI signingEndpoint) {
        return credentialResolver.resolve(provider.secretReference()).flatMap(credentials -> Mono.fromCallable(() -> {
            S3Settings settings = S3Settings.from(provider);
            URI endpoint = signingEndpoint == null ? settings.endpoint()
                : endpointWithoutBucketPrefix(signingEndpoint, settings.bucket());
            try (S3Presigner presigner = S3Presigner.builder().region(Region.of(settings.region()))
                .endpointOverride(endpoint).credentialsProvider(credentials).build()) {
                GetObjectPresignRequest presign = GetObjectPresignRequest.builder().signatureDuration(timeout)
                    .getObjectRequest(GetObjectRequest.builder().bucket(settings.bucket()).key(objectKey).build()).build();
                return new StorageReadIntent("GET", presigner.presignGetObject(presign).url().toString(), Instant.now().plus(timeout));
            }
        })).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * S3 virtual-hosted addressing adds the bucket to the endpoint host. A CDN
     * endpoint may already contain that bucket prefix, so remove it before
     * handing the endpoint to the SDK; the SDK then adds it exactly once.
     */
    private URI endpointWithoutBucketPrefix(URI endpoint, String bucket) {
        String host = endpoint.getHost();
        String prefix = bucket + ".";
        if (host == null || !host.regionMatches(true, 0, prefix, 0, prefix.length())) return endpoint;
        try {
            return new URI(endpoint.getScheme(), endpoint.getUserInfo(), host.substring(prefix.length()),
                endpoint.getPort(), endpoint.getPath(), endpoint.getQuery(), endpoint.getFragment());
        } catch (java.net.URISyntaxException error) {
            throw new IllegalArgumentException("S3 signing endpoint 无效", error);
        }
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

    @Override
    public Mono<StorageObjectMetadata> write(StorageProvider provider, String objectKey,
                                             String mediaType, byte[] content) {
        return credentialResolver.resolve(provider.secretReference()).flatMap(credentials -> Mono.fromCallable(() -> {
            S3Settings settings = S3Settings.from(provider);
            return withClient(settings, credentials, client -> {
                client.putObject(PutObjectRequest.builder().bucket(settings.bucket()).key(objectKey)
                    .contentType(mediaType).contentLength((long) content.length).build(),
                    software.amazon.awssdk.core.sync.RequestBody.fromBytes(content));
                return new StorageObjectMetadata(objectKey, content.length, mediaType, null, null);
            });
        })).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> deleteObject(StorageProvider provider, String objectKey) {
        return credentialResolver.resolve(provider.secretReference()).flatMap(credentials -> Mono.fromRunnable(() -> {
            S3Settings settings = S3Settings.from(provider);
            withClient(settings, credentials, client -> client.deleteObject(DeleteObjectRequest.builder()
                .bucket(settings.bucket()).key(objectKey).build()));
        })).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<StorageProviderProbeResult> probe(StorageProvider provider) {
        return credentialResolver.resolve(provider.secretReference()).flatMap(credentials -> Mono.fromCallable(() -> {
            S3Settings settings = S3Settings.from(provider);
            String key = ".ikaros-probe/" + provider.id() + "/" + java.util.UUID.randomUUID();
            try (S3Client client = buildClient(settings, credentials)) {
                boolean created = false;
                try {
                    client.putObject(PutObjectRequest.builder().bucket(settings.bucket()).key(key)
                        .contentType("application/octet-stream").build(),
                        software.amazon.awssdk.core.sync.RequestBody.empty());
                    created = true;
                    var object = client.headObject(HeadObjectRequest.builder().bucket(settings.bucket()).key(key).build());
                    return new StorageProviderProbeResult(provider.id(), StorageProviderProbeStatus.HEALTHY,
                        true, object != null, true, Instant.now(), null);
                } finally {
                    if (created) client.deleteObject(DeleteObjectRequest.builder().bucket(settings.bucket()).key(key).build());
                }
            }
        })).subscribeOn(Schedulers.boundedElastic())
            .onErrorResume(error -> {
                // 探测失败的具体原因（连接/认证/Endpoint/权限等）只在 debug 级别输出，
                // 便于排查，同时避免默认日志噪音与凭据外泄。
                LOGGER.debug("Storage Provider[{}] probe failed (type={}, endpoint={}, bucket={})",
                    provider.providerKey(), provider.providerType(),
                    provider.metadata().get("endpoint"), provider.metadata().get("bucket"), error);
                return Mono.just(new StorageProviderProbeResult(provider.id(),
                    StorageProviderProbeStatus.FAILED, false, false, false, Instant.now(), classifyProbeError(error)));
            });
    }

    private String classifyProbeError(Throwable error) {
        String name = error.getClass().getSimpleName().toLowerCase();
        if (name.contains("credential") || name.contains("accessdenied") || name.contains("auth")) return "AUTH_FAILED";
        if (name.contains("timeout") || name.contains("sdkclient")) return "NETWORK_UNAVAILABLE";
        return "PROVIDER_UNAVAILABLE";
    }

    /**
     * Builds a client for third-party S3-compatible providers (Aliyun OSS,
     * Tencent COS, MinIO...). Chunked transfer encoding is disabled because the
     * SDK defaults to {@code aws-chunked} since 2.30, and those providers reject
     * it with "MultiChunkedEncoding ... is not supported". Flexible checksums
     * are sent only when the operation requires them, so no CRC32 trailer or
     * checksum header is added for plain PUT/GET.
     */
    static S3Client buildClient(S3Settings settings, AwsCredentialsProvider credentials) {
        return S3Client.builder().region(Region.of(settings.region()))
            .endpointOverride(settings.endpoint()).credentialsProvider(credentials)
            .serviceConfiguration(S3Configuration.builder().chunkedEncodingEnabled(false).build())
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
            .build();
    }

    private <T> T withClient(S3Settings settings, AwsCredentialsProvider credentials,
                             java.util.function.Function<S3Client, T> action) {
        try (S3Client client = buildClient(settings, credentials)) {
            return action.apply(client);
        }
    }

    record S3Settings(String bucket, String region, URI endpoint) {
        static S3Settings from(StorageProvider provider) {
            Map<String, Object> metadata = provider.metadata();
            String bucket = required(metadata, "bucket");
            String region = String.valueOf(metadata.getOrDefault("region", "us-east-1"));
            return new S3Settings(bucket, region, endpoint(required(metadata, "endpoint")));
        }

        /**
         * Vendor endpoints are commonly written without a scheme (for example
         * {@code oss-cn-hangzhou.aliyuncs.com}). The AWS SDK requires an
         * absolute endpoint URI, so default a missing scheme to {@code https}.
         */
        private static URI endpoint(String endpointText) {
            String normalized = endpointText.contains("://") ? endpointText : "https://" + endpointText;
            try {
                URI parsed = URI.create(normalized);
                if (parsed.getHost() == null) {
                    throw new IllegalArgumentException("S3 Provider endpoint 无效: " + endpointText);
                }
                return parsed;
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("S3 Provider endpoint 无效: " + endpointText, error);
            }
        }

        private static String required(Map<String, Object> metadata, String key) {
            return Optional.ofNullable(metadata.get(key)).map(Object::toString).filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("S3 Provider metadata 缺少 " + key));
        }
    }
}
