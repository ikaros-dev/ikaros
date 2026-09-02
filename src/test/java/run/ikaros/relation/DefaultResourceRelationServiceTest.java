package run.ikaros.relation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.audit.AuditService;
import run.ikaros.common.ConflictException;
import run.ikaros.resource.ResourceEntity;
import run.ikaros.resource.ResourceLifecycle;
import run.ikaros.resource.ResourceRepository;
import run.ikaros.resource.ResourceType;

/** 验证 Resource 关系的归属、自关联与删除规则。 */
class DefaultResourceRelationServiceTest {
    private ResourceRepository resourceRepository;
    private ResourceRelationRepository relationRepository;
    private AuditService auditService;
    private DefaultResourceRelationService service;

    @BeforeEach
    void setUp() {
        resourceRepository = mock(ResourceRepository.class);
        relationRepository = mock(ResourceRelationRepository.class);
        auditService = mock(AuditService.class);
        service = new DefaultResourceRelationService(resourceRepository, relationRepository, auditService);
    }

    @Test
    void createsRelationOnlyBetweenOwnedResources() {
        UUID ownerId = UUID.randomUUID(); UUID sourceId = UUID.randomUUID(); UUID targetId = UUID.randomUUID();
        Instant now = Instant.now();
        when(resourceRepository.findByIdAndOwnerId(sourceId, ownerId)).thenReturn(Mono.just(resource(sourceId, ownerId, now)));
        when(resourceRepository.findByIdAndOwnerId(targetId, ownerId)).thenReturn(Mono.just(resource(targetId, ownerId, now)));
        ResourceRelationEntity saved = new ResourceRelationEntity(UUID.randomUUID(), sourceId, targetId,
            ResourceRelationType.CONTAINS, 2, now, 0L);
        when(relationRepository.save(any())).thenReturn(Mono.just(saved));
        when(auditService.record(eq(ownerId), eq("resource.relation.create"), eq("RESOURCE"), eq(sourceId), eq("{}")))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.create(ownerId, sourceId, new CreateResourceRelationRequest(targetId,
                ResourceRelationType.CONTAINS, 2)))
            .assertNext(view -> assertThat(view.targetResourceId()).isEqualTo(targetId)).verifyComplete();
    }

    @Test
    void rejectsSelfRelationBeforeAccessingPersistence() {
        UUID ownerId = UUID.randomUUID(); UUID resourceId = UUID.randomUUID();
        StepVerifier.create(service.create(ownerId, resourceId, new CreateResourceRelationRequest(resourceId,
                ResourceRelationType.RELATED_TO, 0)))
            .expectError(ConflictException.class).verify();
    }

    @Test
    void listsAndRemovesOnlyRelationsOwnedBySourceResource() {
        UUID ownerId = UUID.randomUUID(); UUID sourceId = UUID.randomUUID(); UUID targetId = UUID.randomUUID();
        UUID relationId = UUID.randomUUID(); Instant now = Instant.now();
        ResourceEntity source = resource(sourceId, ownerId, now);
        ResourceRelationEntity relation = new ResourceRelationEntity(relationId, sourceId, targetId,
            ResourceRelationType.RELATED_TO, 0, now, 0L);
        when(resourceRepository.findByIdAndOwnerId(sourceId, ownerId)).thenReturn(Mono.just(source));
        when(relationRepository.findAllBySourceResourceIdOrderByRelationTypeAscPositionAsc(sourceId))
            .thenReturn(Flux.just(relation));
        when(relationRepository.findById(relationId)).thenReturn(Mono.just(relation));
        when(relationRepository.delete(relation)).thenReturn(Mono.empty());
        when(auditService.record(eq(ownerId), eq("resource.relation.delete"), eq("RESOURCE"), eq(sourceId), eq("{}")))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.list(ownerId, sourceId)).expectNextCount(1).verifyComplete();
        StepVerifier.create(service.remove(ownerId, sourceId, relationId)).verifyComplete();
        verify(relationRepository).delete(relation);
    }

    private ResourceEntity resource(UUID id, UUID ownerId, Instant now) {
        return new ResourceEntity(id, ownerId, ResourceType.DOCUMENT, ResourceLifecycle.ACTIVE, now, now, null, 0L);
    }
}
