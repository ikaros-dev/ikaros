package run.ikaros.ingestion;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.resource.api.MetadataSource;
import run.ikaros.resource.api.ResourceMetadataService;
import run.ikaros.resource.api.ResourceMetadataView;
import run.ikaros.resource.api.ResourceOwnershipQuery;

@Service
public class DefaultMetadataSyncService implements MetadataSyncService {
    private final MetadataSyncSourceRepository sources;
    private final ResourceOwnershipQuery resources;
    private final ResourceMetadataService metadata;
    private final MetadataCandidateService candidates;

    public DefaultMetadataSyncService(MetadataSyncSourceRepository sources, ResourceOwnershipQuery resources,
                                      ResourceMetadataService metadata, MetadataCandidateService candidates) {
        this.sources = sources;
        this.resources = resources;
        this.metadata = metadata;
        this.candidates = candidates;
    }

    @Override
    public Mono<MetadataRefreshResult> detect(UUID ownerId, UUID syncSourceId, DetectMetadataUpdateRequest request) {
        return sources.findByIdAndOwnerId(syncSourceId, ownerId)
            .filter(source -> MetadataSyncSourceStatus.ENABLED.name().equals(source.status()))
            .switchIfEmpty(Mono.error(new NotFoundException("启用的元数据同步来源不存在或无权访问")))
            .flatMap(source -> resources.requireOwned(ownerId, request.resourceId())
                .then(metadata.list(ownerId, request.resourceId()).filter(item -> item.fieldKey()
                    .equals(request.fieldKey().trim())).next())
                .flatMap(current -> sameValue(current, request.value().trim())
                    ? Mono.just(new MetadataRefreshResult("UNCHANGED", null))
                    : createCandidate(ownerId, source, request))
                .switchIfEmpty(Mono.defer(() -> createCandidate(ownerId, source, request))));
    }

    private boolean sameValue(ResourceMetadataView current, String incoming) {
        return incoming.equals(current.value());
    }

    private Mono<MetadataRefreshResult> createCandidate(UUID ownerId, MetadataSyncSourceEntity source,
                                                        DetectMetadataUpdateRequest request) {
        String sourceReference = request.sourceReference() == null || request.sourceReference().isBlank()
            ? source.providerKey() : request.sourceReference().trim();
        return candidates.submit(ownerId, request.resourceId(), new SubmitMetadataCandidateRequest(
            request.fieldKey().trim(), request.value().trim(), MetadataSource.PROVIDER, sourceReference,
            request.confidence()))
            .map(candidate -> new MetadataRefreshResult("CANDIDATE_CREATED", candidate));
    }
}
