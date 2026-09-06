package run.ikaros.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.resource.ResourceEntity;
import run.ikaros.resource.ResourceLifecycle;
import run.ikaros.resource.ResourceRepository;
import run.ikaros.resource.ResourceType;

/** 验证统一消费进度的幂等更新和参数规则。 */
class DefaultResourceProgressServiceTest {
    private ResourceRepository resourceRepository;
    private ResourceProgressRepository progressRepository;
    private DefaultResourceProgressService service;

    @BeforeEach
    void setUp() {
        resourceRepository = mock(ResourceRepository.class);
        progressRepository = mock(ResourceProgressRepository.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new DefaultResourceProgressService(resourceRepository, progressRepository, transaction);
    }

    @Test
    void updatesExistingProgressRecord() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceProgressEntity existing = new ResourceProgressEntity(UUID.randomUUID(), ownerId, resourceId,
            ProgressType.VIDEO_SECONDS, 10L, 100L, false, now, 0L);
        ResourceProgressEntity saved = new ResourceProgressEntity(existing.id(), ownerId, resourceId,
            ProgressType.VIDEO_SECONDS, 50L, 100L, false, now, 1L);
        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Mono.just(resource(ownerId, resourceId, now)));
        when(progressRepository.findByOwnerIdAndResourceIdAndProgressType(ownerId, resourceId, ProgressType.VIDEO_SECONDS))
            .thenReturn(Mono.just(existing));
        when(progressRepository.save(any(ResourceProgressEntity.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(service.set(ownerId, resourceId, new SetProgressRequest(
                ProgressType.VIDEO_SECONDS, 50L, 100L, false)))
            .assertNext(view -> assertThat(view.position()).isEqualTo(50L))
            .verifyComplete();
    }

    @Test
    void rejectsPositionBeyondTotal() {
        StepVerifier.create(service.set(UUID.randomUUID(), UUID.randomUUID(), new SetProgressRequest(
                ProgressType.READING_PERCENT, 101L, 100L, false)))
            .expectErrorMessage("当前进度不能超过总进度")
            .verify();
    }

    private ResourceEntity resource(UUID ownerId, UUID resourceId, Instant now) {
        return new ResourceEntity(resourceId, ownerId, ResourceType.BOOK, ResourceLifecycle.ACTIVE, now, now, null, 0L);
    }
}
