package run.ikaros.relation;

import run.ikaros.resource.api.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.operations.api.AuditService;
import run.ikaros.common.ConflictException;
import run.ikaros.resource.ResourceEntity;
import run.ikaros.resource.api.ResourceLifecycle;
import run.ikaros.resource.ResourceRepository;
import run.ikaros.resource.api.ResourceType;

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

    @Test
    void returnsEmptyRelationsAndRejectsUnknownSource() {
        UUID ownerId = UUID.randomUUID(); UUID sourceId = UUID.randomUUID(); Instant now = Instant.now();
        when(resourceRepository.findByIdAndOwnerId(sourceId, ownerId)).thenReturn(Mono.just(resource(sourceId, ownerId, now)));
        when(relationRepository.findAllBySourceResourceIdOrderByRelationTypeAscPositionAsc(sourceId))
            .thenReturn(Flux.empty());
        StepVerifier.create(service.list(ownerId, sourceId)).verifyComplete();

        UUID unknown = UUID.randomUUID();
        when(resourceRepository.findByIdAndOwnerId(unknown, ownerId)).thenReturn(Mono.empty());
        StepVerifier.create(service.list(ownerId, unknown)).expectErrorMessage("资源不存在或无权访问").verify();
        verify(relationRepository, never()).findAllBySourceResourceIdOrderByRelationTypeAscPositionAsc(unknown);
    }

    @Test
    void rejectsUnknownRelationWithoutDeletingOrAuditing() {
        UUID ownerId = UUID.randomUUID(), sourceId = UUID.randomUUID(), relationId = UUID.randomUUID();
        Instant now = Instant.now();
        when(resourceRepository.findByIdAndOwnerId(sourceId, ownerId)).thenReturn(Mono.just(resource(sourceId, ownerId, now)));
        when(relationRepository.findById(relationId)).thenReturn(Mono.empty());

        StepVerifier.create(service.remove(ownerId, sourceId, relationId))
            .expectErrorMessage("资源关系不存在").verify();
        verify(relationRepository, never()).delete(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsInvalidRelationRequestBeforePersistence() {
        UUID ownerId = UUID.randomUUID(), sourceId = UUID.randomUUID(), targetId = UUID.randomUUID();
        StepVerifier.create(service.create(ownerId, sourceId,
                new CreateResourceRelationRequest(targetId, null, -1)))
            .expectErrorMessage("资源关系请求不合法").verify();
        verify(relationRepository, never()).save(any());
    }

    private ResourceEntity resource(UUID id, UUID ownerId, Instant now) {
        return new ResourceEntity(id, ownerId, ResourceType.DOCUMENT, ResourceLifecycle.ACTIVE, now, now, null, 0L);
    }
}
