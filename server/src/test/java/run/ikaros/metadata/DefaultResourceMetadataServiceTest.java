package run.ikaros.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.audit.AuditService;
import run.ikaros.resource.ResourceEntity;
import run.ikaros.resource.ResourceLifecycle;
import run.ikaros.resource.ResourceRepository;
import run.ikaros.resource.ResourceType;

/** 验证人工元数据不会被自动来源静默覆盖。 */
class DefaultResourceMetadataServiceTest {
    private ResourceRepository resourceRepository; private ResourceMetadataRepository metadataRepository;
    private AuditService auditService; private DefaultResourceMetadataService service;
    @BeforeEach void setUp(){resourceRepository=mock(ResourceRepository.class);metadataRepository=mock(ResourceMetadataRepository.class);auditService=mock(AuditService.class);service=new DefaultResourceMetadataService(resourceRepository,metadataRepository,auditService);}
    @Test void preservesManualLockAndAllowsExplicitRestore() {
        UUID owner=UUID.randomUUID(), resource=UUID.randomUUID(); Instant now=Instant.now();
        ResourceEntity entity=new ResourceEntity(resource,owner,ResourceType.DOCUMENT,ResourceLifecycle.ACTIVE,now,now,null,0L);
        ResourceMetadataEntity manual=new ResourceMetadataEntity(UUID.randomUUID(),resource,"title","人工标题",MetadataSource.USER,null,true,now,0L);
        when(resourceRepository.findByIdAndOwnerId(resource,owner)).thenReturn(Mono.just(entity));
        when(metadataRepository.findByResourceIdAndFieldKey(resource,"title")).thenReturn(Mono.just(manual));
        when(metadataRepository.save(any())).thenAnswer(i->Mono.just(i.getArgument(0)));
        when(auditService.record(eq(owner),any(),eq("RESOURCE"),eq(resource),eq("{}"))).thenReturn(Mono.empty());
        StepVerifier.create(service.applyAutomatic(owner,resource,"title",new AutomaticMetadataRequest("外部标题",MetadataSource.PROVIDER,"tmdb")))
            .assertNext(view->{assertThat(view.applied()).isFalse();assertThat(view.value()).isEqualTo("人工标题");}).verifyComplete();
        StepVerifier.create(service.restoreAutomatic(owner,resource,"title"))
            .assertNext(view->assertThat(view.manuallyLocked()).isFalse()).verifyComplete();
    }
    @Test void listsOwnedMetadataFields() {
        UUID owner=UUID.randomUUID(), resource=UUID.randomUUID(); Instant now=Instant.now();
        when(resourceRepository.findByIdAndOwnerId(resource,owner)).thenReturn(Mono.just(new ResourceEntity(resource,owner,ResourceType.DOCUMENT,ResourceLifecycle.ACTIVE,now,now,null,0L)));
        when(metadataRepository.findAllByResourceIdOrderByFieldKeyAsc(resource)).thenReturn(Flux.just(new ResourceMetadataEntity(UUID.randomUUID(),resource,"title","标题",MetadataSource.PROVIDER,"tmdb",false,now,0L)));
        StepVerifier.create(service.list(owner,resource)).assertNext(view->assertThat(view.source()).isEqualTo(MetadataSource.PROVIDER)).verifyComplete();
    }
}
