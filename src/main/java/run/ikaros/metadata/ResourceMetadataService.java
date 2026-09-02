package run.ikaros.metadata;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Resource 元数据来源与人工覆盖规则的业务边界。 */
public interface ResourceMetadataService {
    Mono<ResourceMetadataView> setManual(UUID ownerId, UUID resourceId, String fieldKey, MetadataValueRequest request);
    Mono<ResourceMetadataView> applyAutomatic(UUID ownerId, UUID resourceId, String fieldKey,
                                               AutomaticMetadataRequest request);
    Mono<ResourceMetadataView> restoreAutomatic(UUID ownerId, UUID resourceId, String fieldKey);
    Flux<ResourceMetadataView> list(UUID ownerId, UUID resourceId);
}
