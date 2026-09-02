package run.ikaros.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.audit.AuditService;

/**
 * 验证 Resource 收藏的幂等、隔离和审计行为。
 */
class DefaultFavoriteServiceTest {
    private ResourceRepository resourceRepository;
    private FavoriteRepository favoriteRepository;
    private AuditService auditService;
    private DefaultFavoriteService service;

    @BeforeEach
    void setUp() {
        resourceRepository = mock(ResourceRepository.class);
        favoriteRepository = mock(FavoriteRepository.class);
        auditService = mock(AuditService.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(org.mockito.ArgumentMatchers.any(Mono.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        service = new DefaultFavoriteService(resourceRepository, favoriteRepository, auditService, transaction);
    }

    @Test
    void addsFavoriteAndWritesAudit() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Mono.just(
            new ResourceEntity(resourceId, ownerId, ResourceType.BOOK, ResourceLifecycle.ACTIVE, now, now, null, 0L)));
        when(favoriteRepository.findByOwnerIdAndResourceId(ownerId, resourceId)).thenReturn(Mono.empty());
        when(favoriteRepository.save(org.mockito.ArgumentMatchers.any(FavoriteEntity.class))).thenReturn(Mono.just(
            new FavoriteEntity(UUID.randomUUID(), ownerId, resourceId, now, 0L)));
        when(auditService.record(ownerId, "resource.favorite.add", "RESOURCE", resourceId, "{}"))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.add(ownerId, resourceId))
            .assertNext(view -> assertThat(view.favorite()).isTrue())
            .verifyComplete();

        verify(auditService).record(ownerId, "resource.favorite.add", "RESOURCE", resourceId, "{}");
    }

    @Test
    void repeatedAddIsIdempotentWithoutAnotherSave() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Mono.just(
            new ResourceEntity(resourceId, ownerId, ResourceType.BOOK, ResourceLifecycle.ACTIVE, now, now, null, 0L)));
        when(favoriteRepository.findByOwnerIdAndResourceId(ownerId, resourceId)).thenReturn(Mono.just(
            new FavoriteEntity(UUID.randomUUID(), ownerId, resourceId, now, 0L)));

        StepVerifier.create(service.add(ownerId, resourceId))
            .assertNext(view -> assertThat(view.favorite()).isTrue())
            .verifyComplete();
    }

    @Test
    void returnsFalseWhenResourceIsNotFavorited() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Mono.just(
            new ResourceEntity(resourceId, ownerId, ResourceType.BOOK, ResourceLifecycle.ACTIVE, now, now, null, 0L)));
        when(favoriteRepository.findByOwnerIdAndResourceId(ownerId, resourceId)).thenReturn(Mono.empty());

        StepVerifier.create(service.get(ownerId, resourceId))
            .assertNext(view -> assertThat(view.favorite()).isFalse())
            .verifyComplete();
    }
}
