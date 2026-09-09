package run.ikaros.collection;

import run.ikaros.resource.api.*;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.operations.api.AuditService;
import run.ikaros.common.ConflictException;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.integration.api.EventAppendRequest;
import run.ikaros.resource.ResourceEntity;
import run.ikaros.resource.api.ResourceLifecycle;
import run.ikaros.resource.ResourceRepository;
import run.ikaros.resource.api.ResourceType;

class DefaultCollectionServiceTest {
    @Test
    void publishesCollectionAndMembershipEvents() {
        UUID ownerId = UUID.randomUUID();
        UUID collectionId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        CollectionRepository collections = mock(CollectionRepository.class);
        CollectionResourceRepository members = mock(CollectionResourceRepository.class);
        ResourceRepository resources = mock(ResourceRepository.class);
        AuditService audit = mock(AuditService.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        DurableEventPublisher events = mock(DurableEventPublisher.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CollectionEntity collection = new CollectionEntity(collectionId, ownerId, null, "收藏", null, now, now, 0L);
        when(collections.save(any(CollectionEntity.class))).thenReturn(Mono.just(collection));
        when(collections.findByIdAndOwnerId(collectionId, ownerId)).thenReturn(Mono.just(collection));
        ResourceEntity resource = new ResourceEntity(resourceId, ownerId, ResourceType.BOOK,
            ResourceLifecycle.ACTIVE, now, now, null, 0L);
        when(resources.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Mono.just(resource));
        when(members.save(any(CollectionResourceEntity.class))).thenReturn(Mono.just(
            new CollectionResourceEntity(UUID.randomUUID(), collectionId, resourceId, 0, now, 0L)));
        when(members.deleteByCollectionIdAndResourceId(collectionId, resourceId)).thenReturn(Mono.empty());
        when(audit.record(eq(ownerId), any(), eq("COLLECTION"), eq(collectionId), any())).thenReturn(Mono.empty());
        when(events.append(any(EventAppendRequest.class))).thenReturn(Mono.empty());

        DefaultCollectionService service = new DefaultCollectionService(collections, members, resources, audit,
            transaction, events);
        StepVerifier.create(service.create(ownerId, new CreateCollectionRequest("收藏", null)))
            .expectNextCount(1).verifyComplete();
        StepVerifier.create(service.addResource(ownerId, collectionId, resourceId, 0)).verifyComplete();
        StepVerifier.create(service.removeResource(ownerId, collectionId, resourceId)).verifyComplete();

        verify(events).append(argThat(request -> request.eventType().equals("resource.collection.created")
            && request.producerSubsystem().equals("resource") && request.subjectType().equals("collection")
            && request.subjectId().equals(collectionId)));
        verify(events).append(argThat(request -> request.eventType().equals("resource.collection.member-added")
            && request.producerSubsystem().equals("resource") && request.subjectType().equals("collection")
            && request.subjectId().equals(collectionId)));
        verify(events).append(argThat(request -> request.eventType().equals("resource.collection.member-removed")
            && request.producerSubsystem().equals("resource") && request.subjectType().equals("collection")
            && request.subjectId().equals(collectionId)));
    }

    @Test
    void updatesCollectionWithOwnerAndVersionChecks() {
        UUID ownerId = UUID.randomUUID();
        UUID collectionId = UUID.randomUUID();
        Instant now = Instant.now();
        CollectionRepository collections = mock(CollectionRepository.class);
        CollectionResourceRepository members = mock(CollectionResourceRepository.class);
        ResourceRepository resources = mock(ResourceRepository.class);
        AuditService audit = mock(AuditService.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CollectionEntity current = new CollectionEntity(collectionId, ownerId, null, "旧名", null, now, now, 1L);
        CollectionEntity updated = new CollectionEntity(collectionId, ownerId, null, "新名", "描述", now, now, 2L);
        when(collections.findByIdAndOwnerId(collectionId, ownerId)).thenReturn(Mono.just(current));
        when(collections.save(any(CollectionEntity.class))).thenReturn(Mono.just(updated));
        when(audit.record(ownerId, "collection.update", "COLLECTION", collectionId, "{}")).thenReturn(Mono.empty());
        DefaultCollectionService service = new DefaultCollectionService(collections, members, resources, audit, transaction);

        StepVerifier.create(service.update(ownerId, collectionId, new UpdateCollectionRequest("新名", "描述", 1L)))
            .assertNext(view -> assertThat(view.name()).isEqualTo("新名")).verifyComplete();
        verify(collections).save(argThat(value -> value.name().equals("新名") && value.version().equals(1L)));
    }

    @Test
    void reordersAllCollectionMembersAndRejectsPartialOrder() {
        UUID ownerId = UUID.randomUUID(), collectionId = UUID.randomUUID();
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        Instant now = Instant.now();
        CollectionRepository collections = mock(CollectionRepository.class);
        CollectionResourceRepository members = mock(CollectionResourceRepository.class);
        ResourceRepository resources = mock(ResourceRepository.class);
        AuditService audit = mock(AuditService.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(collections.findByIdAndOwnerId(collectionId, ownerId)).thenReturn(Mono.just(
            new CollectionEntity(collectionId, ownerId, "收藏", null, now, now, 0L)));
        when(members.findAllByCollectionId(collectionId)).thenReturn(reactor.core.publisher.Flux.just(
            new CollectionResourceEntity(UUID.randomUUID(), collectionId, first, 0, now, 0L),
            new CollectionResourceEntity(UUID.randomUUID(), collectionId, second, 1, now, 0L)));
        when(members.saveAll(any(Iterable.class))).thenReturn(reactor.core.publisher.Flux.empty());
        when(audit.record(ownerId, "collection.resource.reorder", "COLLECTION", collectionId, "{}"))
            .thenReturn(Mono.empty());
        DefaultCollectionService service = new DefaultCollectionService(collections, members, resources, audit, transaction);

        StepVerifier.create(service.reorderResources(ownerId, collectionId, java.util.List.of(second, first)))
            .verifyComplete();
        StepVerifier.create(service.reorderResources(ownerId, collectionId, java.util.List.of(first)))
            .expectError(ConflictException.class).verify();
        verify(members).saveAll(any(Iterable.class));
    }
}
