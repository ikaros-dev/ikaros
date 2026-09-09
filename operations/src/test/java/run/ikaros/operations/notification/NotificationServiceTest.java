package run.ikaros.operations.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;
import run.ikaros.integration.api.DurableEvent;

class NotificationServiceTest {
    @Test
    void preferenceDefaultsToEnabledAndCanBeUpdated() {
        NotificationPreferenceRepository repository = mock(NotificationPreferenceRepository.class);
        UUID recipient = UUID.randomUUID();
        when(repository.findById(recipient)).thenReturn(Mono.empty());
        NotificationPreferenceService service = new DefaultNotificationPreferenceService(repository);

        StepVerifier.create(service.get(recipient))
            .assertNext(value -> assertThat(value).isEqualTo(new NotificationPreferenceView(true, true)))
            .verifyComplete();

        NotificationPreferenceRequest request = new NotificationPreferenceRequest(false, true);
        when(repository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        StepVerifier.create(service.update(recipient, request))
            .assertNext(value -> assertThat(value).isEqualTo(new NotificationPreferenceView(false, true)))
            .verifyComplete();
    }

    @Test
    void terminalTaskEventCreatesNotificationForEventActor() {
        NotificationService notifications = mock(NotificationService.class);
        NotificationPreferenceService preferences = mock(NotificationPreferenceService.class);
        UUID actor = UUID.randomUUID();
        UUID task = UUID.randomUUID();
        DurableEvent event = new DurableEvent(UUID.randomUUID(), "operations.background-task.succeeded", 1,
            "operations", "background_task", task, "{}", Instant.now(), null, null, null, actor);
        when(preferences.enabled(actor, "SUCCEEDED")).thenReturn(Mono.just(true));
        when(notifications.create(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(new TaskNotificationConsumer(notifications, preferences, new ObjectMapper()).consume(event))
            .verifyComplete();
        verify(notifications).create(any());
    }

    @Test
    void disabledFailurePreferenceDoesNotCreateNotification() {
        NotificationService notifications = mock(NotificationService.class);
        NotificationPreferenceService preferences = mock(NotificationPreferenceService.class);
        UUID actor = UUID.randomUUID();
        DurableEvent event = new DurableEvent(UUID.randomUUID(), "operations.background-task.failed", 1,
            "operations", "background_task", UUID.randomUUID(), "{}", Instant.now(), null, null, null, actor);
        when(preferences.enabled(actor, "FAILED")).thenReturn(Mono.just(false));

        StepVerifier.create(new TaskNotificationConsumer(notifications, preferences, new ObjectMapper()).consume(event))
            .verifyComplete();
        verify(notifications, never()).create(any());
    }

    @Test
    void nonTerminalEventIsIgnored() {
        NotificationService notifications = mock(NotificationService.class);
        NotificationPreferenceService preferences = mock(NotificationPreferenceService.class);
        DurableEvent event = new DurableEvent(UUID.randomUUID(), "operations.background-task.started", 1,
            "operations", "background_task", UUID.randomUUID(), "{}", Instant.now(), null, null, null,
            UUID.randomUUID());

        StepVerifier.create(new TaskNotificationConsumer(notifications, preferences, new ObjectMapper()).consume(event))
            .verifyComplete();
        verify(notifications, never()).create(any());
    }

    @Test
    void listingUsesRecipientAndFilters() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationEntity entity = new NotificationEntity(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "TASK", "operations.background-task.succeeded", "done", "body", "NORMAL", "UNREAD", UUID.randomUUID(),
            null, Instant.now(), null, null, 0L);
        UUID recipient = entity.recipientId();
        when(repository.search(recipient, "UNREAD", "TASK", "HIGH", 20, 20)).thenReturn(Flux.just(entity));
        when(repository.countSearch(recipient, "UNREAD", "TASK", "HIGH")).thenReturn(Mono.just(1L));
        NotificationService service = new DefaultNotificationService(repository);

        StepVerifier.create(service.search(recipient, "unread", "task", "high", 1, 20))
            .assertNext(page -> {
                assertThat(page.total()).isEqualTo(1);
                assertThat(page.items()).hasSize(1);
            }).verifyComplete();
    }
}
