package run.ikaros.activity;

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
import run.ikaros.resource.ResourceEntity;
import run.ikaros.resource.ResourceLifecycle;
import run.ikaros.resource.ResourceRepository;
import run.ikaros.resource.ResourceType;

/** 验证 Activity 与 Resource 所有权及审计边界。 */
class DefaultResourceActivityServiceTest {
    private ResourceRepository resourceRepository;
    private ResourceActivityRepository activityRepository;
    private AuditService auditService;
    private DefaultResourceActivityService service;

    @BeforeEach
    void setUp() {
        resourceRepository = mock(ResourceRepository.class);
        activityRepository = mock(ResourceActivityRepository.class);
        auditService = mock(AuditService.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new DefaultResourceActivityService(resourceRepository, activityRepository, auditService, transaction);
    }

    @Test
    void recordsActivityForOwnedResource() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Mono.just(resource(ownerId, resourceId, now)));
        ResourceActivityEntity activity = new ResourceActivityEntity(UUID.randomUUID(), ownerId, resourceId,
            ActivityType.VIEW, "{}", now, 0L);
        when(activityRepository.save(any(ResourceActivityEntity.class))).thenReturn(Mono.just(activity));

        StepVerifier.create(service.record(ownerId, resourceId, new RecordActivityRequest(ActivityType.VIEW, null)))
            .assertNext(view -> assertThat(view.type()).isEqualTo(ActivityType.VIEW))
            .verifyComplete();
    }

    @Test
    void limitsRecentActivities() {
        UUID ownerId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceActivityEntity first = activity(ownerId, now);
        ResourceActivityEntity second = activity(ownerId, now.minusSeconds(1));
        when(activityRepository.findAllByOwnerIdOrderByOccurredAtDesc(ownerId)).thenReturn(Flux.just(first, second));

        StepVerifier.create(service.recent(ownerId, 1))
            .assertNext(activities -> assertThat(activities).singleElement().satisfies(
                value -> assertThat(value.id()).isEqualTo(first.id())))
            .verifyComplete();
    }

    @Test
    void deletesOwnActivityAndAuditsDeletion() {
        UUID ownerId = UUID.randomUUID();
        ResourceActivityEntity activity = activity(ownerId, Instant.now());
        when(activityRepository.findByIdAndOwnerId(activity.id(), ownerId)).thenReturn(Mono.just(activity));
        when(activityRepository.deleteById(activity.id())).thenReturn(Mono.empty());
        when(auditService.record(ownerId, "resource.activity.delete", "RESOURCE_ACTIVITY", activity.id(), "{}"))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.delete(ownerId, activity.id())).verifyComplete();
        verify(auditService).record(ownerId, "resource.activity.delete", "RESOURCE_ACTIVITY", activity.id(), "{}");
    }

    private ResourceEntity resource(UUID ownerId, UUID resourceId, Instant now) {
        return new ResourceEntity(resourceId, ownerId, ResourceType.BOOK, ResourceLifecycle.ACTIVE, now, now, null, 0L);
    }

    private ResourceActivityEntity activity(UUID ownerId, Instant occurredAt) {
        return new ResourceActivityEntity(UUID.randomUUID(), ownerId, UUID.randomUUID(), ActivityType.VIEW,
            "{}", occurredAt, 0L);
    }
}
