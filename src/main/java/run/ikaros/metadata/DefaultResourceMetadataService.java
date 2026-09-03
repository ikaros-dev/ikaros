package run.ikaros.metadata;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.audit.AuditService;
import run.ikaros.common.NotFoundException;
import run.ikaros.resource.ResourceRepository;

/** 默认元数据服务，保证人工确认字段不会被自动来源静默覆盖。 */
@Service
public class DefaultResourceMetadataService implements ResourceMetadataService {
    private final ResourceRepository resourceRepository; private final ResourceMetadataRepository metadataRepository;
    private final AuditService auditService;
    public DefaultResourceMetadataService(ResourceRepository resourceRepository, ResourceMetadataRepository metadataRepository,
                                          AuditService auditService) { this.resourceRepository=resourceRepository; this.metadataRepository=metadataRepository; this.auditService=auditService; }
    @Override public Mono<ResourceMetadataView> setManual(UUID ownerId, UUID resourceId, String fieldKey, MetadataValueRequest request) {
        return owned(ownerId, resourceId).then(metadataRepository.findByResourceIdAndFieldKey(resourceId, fieldKey).defaultIfEmpty(
            new ResourceMetadataEntity(null, resourceId, fieldKey, request.value(), MetadataSource.USER, null, true, Instant.now(), null)))
            .flatMap(current -> metadataRepository.save(new ResourceMetadataEntity(current.id(), resourceId, fieldKey, request.value(), MetadataSource.USER, null, true, Instant.now(), current.version())))
            .flatMap(saved -> auditService.record(ownerId,"resource.metadata.manual.set","RESOURCE",resourceId,"{}").thenReturn(view(saved,true)));
    }
    @Override public Mono<ResourceMetadataView> applyAutomatic(UUID ownerId, UUID resourceId, String fieldKey, AutomaticMetadataRequest request) {
        return owned(ownerId, resourceId).then(metadataRepository.findByResourceIdAndFieldKey(resourceId, fieldKey)
            .flatMap(current -> current.manuallyLocked() ? Mono.just(view(current,false)) : saveAutomatic(current,request))
            .switchIfEmpty(saveAutomatic(new ResourceMetadataEntity(null,resourceId,fieldKey,"",request.source(),null,false,Instant.now(),null),request)));
    }
    @Override public Mono<ResourceMetadataView> restoreAutomatic(UUID ownerId, UUID resourceId, String fieldKey) {
        return owned(ownerId,resourceId).then(metadataRepository.findByResourceIdAndFieldKey(resourceId,fieldKey))
            .switchIfEmpty(Mono.error(new NotFoundException("元数据字段不存在")))
            .flatMap(current -> metadataRepository.save(new ResourceMetadataEntity(current.id(),resourceId,fieldKey,current.value(),current.source(),current.sourceReference(),false,Instant.now(),current.version())))
            .flatMap(saved -> auditService.record(ownerId,"resource.metadata.automatic.restore","RESOURCE",resourceId,"{}").thenReturn(view(saved,true)));
    }
    @Override public Flux<ResourceMetadataView> list(UUID ownerId, UUID resourceId) { return owned(ownerId,resourceId).thenMany(metadataRepository.findAllByResourceIdOrderByFieldKeyAsc(resourceId).take(100).map(value -> view(value,true))); }
    private Mono<ResourceMetadataView> saveAutomatic(ResourceMetadataEntity current, AutomaticMetadataRequest request) { return metadataRepository.save(new ResourceMetadataEntity(current.id(),current.resourceId(),current.fieldKey(),request.value(),request.source(),request.sourceReference(),false,Instant.now(),current.version())).map(saved -> view(saved,true)); }
    private Mono<Void> owned(UUID ownerId, UUID resourceId) { return resourceRepository.findByIdAndOwnerId(resourceId,ownerId).switchIfEmpty(Mono.error(new NotFoundException("资源不存在或无权访问"))).then(); }
    private ResourceMetadataView view(ResourceMetadataEntity value, boolean applied) { return new ResourceMetadataView(value.id(),value.fieldKey(),value.value(),value.source(),value.sourceReference(),value.manuallyLocked(),applied); }
}
