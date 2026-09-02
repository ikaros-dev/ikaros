package run.ikaros.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.audit.AuditService;

/**
 * 验证用户自定义标签的幂等、隔离和审计行为。
 */
class DefaultResourceTagServiceTest {
    private ResourceRepository resourceRepository;
    private ResourceTagRepository tagRepository;
    private AuditService auditService;
    private DefaultResourceTagService service;

    @BeforeEach
    void setUp() {
        resourceRepository = mock(ResourceRepository.class);
        tagRepository = mock(ResourceTagRepository.class);
        auditService = mock(AuditService.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new DefaultResourceTagService(resourceRepository, tagRepository, auditService, transaction);
    }

    @Test
    void addsTagAndWritesAudit() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Mono.just(resource(ownerId, resourceId, now)));
        when(tagRepository.findByOwnerIdAndResourceIdAndName(ownerId, resourceId, "favorite"))
            .thenReturn(Mono.empty());
        ResourceTagEntity tag = new ResourceTagEntity(UUID.randomUUID(), ownerId, resourceId, "favorite", "#fff", now, now, 0L);
        when(tagRepository.save(any(ResourceTagEntity.class))).thenReturn(Mono.just(tag));
        when(auditService.record(ownerId, "resource.tag.add", "RESOURCE", resourceId, "{}")).thenReturn(Mono.empty());

        StepVerifier.create(service.add(ownerId, resourceId, new CreateResourceTagRequest(" favorite ", "#fff")))
            .assertNext(view -> assertThat(view.name()).isEqualTo("favorite"))
            .verifyComplete();
        verify(auditService).record(ownerId, "resource.tag.add", "RESOURCE", resourceId, "{}");
    }

    @Test
    void listsTagsForOwnedResource() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Mono.just(resource(ownerId, resourceId, now)));
        ResourceTagEntity tag = new ResourceTagEntity(UUID.randomUUID(), ownerId, resourceId, "action", null, now, now, 0L);
        when(tagRepository.findAllByOwnerIdAndResourceIdOrderByNameAsc(ownerId, resourceId)).thenReturn(Flux.just(tag));

        StepVerifier.create(service.list(ownerId, resourceId))
            .assertNext(tags -> assertThat(tags).extracting(ResourceTagView::name).containsExactly("action"))
            .verifyComplete();
    }

    @Test
    void removesTagAndWritesAudit() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();
        Instant now = Instant.now();
        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Mono.just(resource(ownerId, resourceId, now)));
        when(tagRepository.findByIdAndOwnerIdAndResourceId(tagId, ownerId, resourceId)).thenReturn(Mono.just(
            new ResourceTagEntity(tagId, ownerId, resourceId, "action", null, now, now, 0L)));
        when(tagRepository.deleteById(tagId)).thenReturn(Mono.empty());
        when(auditService.record(ownerId, "resource.tag.remove", "RESOURCE", resourceId, "{}")).thenReturn(Mono.empty());

        StepVerifier.create(service.remove(ownerId, resourceId, tagId)).verifyComplete();
        verify(tagRepository).deleteById(tagId);
    }

    private ResourceEntity resource(UUID ownerId, UUID resourceId, Instant now) {
        return new ResourceEntity(resourceId, ownerId, ResourceType.BOOK, ResourceLifecycle.ACTIVE, now, now, null, 0L);
    }
}
