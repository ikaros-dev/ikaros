package run.ikaros.resource;

import run.ikaros.resource.api.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.integration.api.EventAppendRequest;

class DefaultUserResourceStateServiceTest {
    @Test
    void publishesChangedFieldsAfterSavingState() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceRepository resources = mock(ResourceRepository.class);
        UserResourceStateRepository states = mock(UserResourceStateRepository.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        DurableEventPublisher events = mock(DurableEventPublisher.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(resources.findByIdAndOwnerId(resourceId, userId)).thenReturn(Mono.just(new ResourceEntity(
            resourceId, userId, ResourceType.BOOK, ResourceLifecycle.ACTIVE, now, now, null, 0L)));
        when(states.findByUserIdAndResourceId(userId, resourceId)).thenReturn(Mono.empty());
        UserResourceStateEntity saved = new UserResourceStateEntity(userId, resourceId, true,
            new BigDecimal("8"), "reading", new BigDecimal("3"), "pages", now, 1L, now);
        when(states.save(any(UserResourceStateEntity.class))).thenReturn(Mono.just(saved));
        when(events.append(any(EventAppendRequest.class))).thenReturn(Mono.empty());

        DefaultUserResourceStateService service = new DefaultUserResourceStateService(resources, states, transaction, events);
        StepVerifier.create(service.set(userId, resourceId,
                new UserResourceStateRequest(true, new BigDecimal("8"), "reading",
                    new BigDecimal("3"), "pages")))
            .expectNextCount(1).verifyComplete();

        verify(events).append(argThat(request -> request.eventType().equals("resource.user-state.changed")
            && request.producerSubsystem().equals("resource") && request.subjectType().equals("resource")
            && request.subjectId().equals(resourceId)));
    }

    @Test
    void rejectsUnknownResourceBeforeReadingState() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        ResourceRepository resources = mock(ResourceRepository.class);
        UserResourceStateRepository states = mock(UserResourceStateRepository.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(resources.findByIdAndOwnerId(resourceId, userId)).thenReturn(Mono.empty());

        DefaultUserResourceStateService service = new DefaultUserResourceStateService(resources, states, transaction);

        StepVerifier.create(service.set(userId, resourceId,
                new UserResourceStateRequest(false, new BigDecimal("9"), null, null, null)))
            .expectErrorMessage("资源不存在或无权访问").verify();
        verify(states, never()).findByUserIdAndResourceId(userId, resourceId);
    }

    @Test
    void rejectsRatingOutsideContract() {
        DefaultUserResourceStateService service = new DefaultUserResourceStateService(
            mock(ResourceRepository.class), mock(UserResourceStateRepository.class), mock(TransactionalOperator.class));

        StepVerifier.create(service.set(UUID.randomUUID(), UUID.randomUUID(),
                new UserResourceStateRequest(false, new BigDecimal("10.01"), null, null, null)))
            .expectErrorMessage("评分必须介于 0 和 10 之间").verify();
    }
}
