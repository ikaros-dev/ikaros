package run.ikaros.collection;

import static org.mockito.ArgumentMatchers.any;
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
import run.ikaros.audit.AuditService;
import run.ikaros.event.DurableEventService;
import run.ikaros.resource.ResourceEntity;
import run.ikaros.resource.ResourceLifecycle;
import run.ikaros.resource.ResourceRepository;
import run.ikaros.resource.ResourceType;

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
        DurableEventService events = mock(DurableEventService.class);
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
        when(events.append(any(), eq(1), any(), eq(collectionId), any())).thenReturn(Mono.empty());

        DefaultCollectionService service = new DefaultCollectionService(collections, members, resources, audit,
            transaction, events);
        StepVerifier.create(service.create(ownerId, new CreateCollectionRequest("收藏", null)))
            .expectNextCount(1).verifyComplete();
        StepVerifier.create(service.addResource(ownerId, collectionId, resourceId, 0)).verifyComplete();
        StepVerifier.create(service.removeResource(ownerId, collectionId, resourceId)).verifyComplete();

        verify(events).append(eq("resource.collection.created"), eq(1), eq("collection"), eq(collectionId),
            eq("{\"collection_id\":\"" + collectionId + "\",\"kind\":\"library\",\"mode\":\"STATIC\"}"));
        verify(events).append(eq("resource.collection.member-added"), eq(1), eq("collection"), eq(collectionId),
            eq("{\"collection_id\":\"" + collectionId + "\",\"resource_id\":\"" + resourceId + "\"}"));
        verify(events).append(eq("resource.collection.member-removed"), eq(1), eq("collection"), eq(collectionId),
            eq("{\"collection_id\":\"" + collectionId + "\",\"resource_id\":\"" + resourceId + "\"}"));
    }
}
