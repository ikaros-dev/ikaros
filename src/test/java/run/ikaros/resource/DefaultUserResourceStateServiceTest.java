package run.ikaros.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.event.DurableEventService;

class DefaultUserResourceStateServiceTest {
    @Test
    void publishesChangedFieldsAfterSavingState() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceRepository resources = mock(ResourceRepository.class);
        UserResourceStateRepository states = mock(UserResourceStateRepository.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        DurableEventService events = mock(DurableEventService.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(resources.findByIdAndOwnerId(resourceId, userId)).thenReturn(Mono.just(new ResourceEntity(
            resourceId, userId, ResourceType.BOOK, ResourceLifecycle.ACTIVE, now, now, null, 0L)));
        when(states.findByUserIdAndResourceId(userId, resourceId)).thenReturn(Mono.empty());
        UserResourceStateEntity saved = new UserResourceStateEntity(userId, resourceId, true,
            new BigDecimal("8"), "reading", new BigDecimal("3"), "pages", now, 1L, now);
        when(states.save(any(UserResourceStateEntity.class))).thenReturn(Mono.just(saved));
        when(events.append(any(), eq(1), eq("resource"), eq(resourceId), any())).thenReturn(Mono.empty());

        DefaultUserResourceStateService service = new DefaultUserResourceStateService(resources, states, transaction, events);
        StepVerifier.create(service.set(userId, resourceId,
                new UserResourceStateRequest(true, new BigDecimal("8"), "reading",
                    new BigDecimal("3"), "pages")))
            .expectNextCount(1).verifyComplete();

        verify(events).append(eq("resource.user-state.changed"), eq(1), eq("resource"), eq(resourceId),
            eq("{\"user_id\":\"" + userId + "\",\"resource_id\":\"" + resourceId
                + "\",\"changed_fields\":[\"favorite\",\"rating\",\"status_code\",\"progress_value\","
                + "\"progress_unit\",\"last_accessed_at\"],\"version\":1}"));
    }
}
