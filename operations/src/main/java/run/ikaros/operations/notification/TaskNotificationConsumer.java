package run.ikaros.operations.notification;

import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import run.ikaros.integration.api.DurableEvent;
import run.ikaros.integration.api.DurableEventConsumer;

@Component
public class TaskNotificationConsumer implements DurableEventConsumer {
    private static final String SUCCEEDED = "operations.background-task.succeeded";
    private static final String FAILED = "operations.background-task.failed";
    private static final String TIMED_OUT = "operations.background-task.timed-out";
    private final NotificationService notifications;
    private final NotificationPreferenceService preferences;
    private final ObjectMapper mapper;

    public TaskNotificationConsumer(NotificationService notifications, NotificationPreferenceService preferences,
                                    ObjectMapper mapper) {
        this.notifications = notifications; this.preferences = preferences;
        this.mapper = mapper;
    }

    @Override
    public String consumerId() {
        return "operations.notification.task-v1";
    }

    @Override
    public Mono<Void> consume(DurableEvent event) {
        if (event == null || !isTaskTerminal(event.eventType()) || event.subjectId() == null) {
            return Mono.empty();
        }
        return recipient(event).flatMap(recipient -> preferences.enabled(recipient, status(event.eventType())))
            .filter(Boolean.TRUE::equals).flatMap(ignored -> recipient(event).flatMap(recipient -> notifications.create(new NotificationEntity(
            null, event.id(), recipient, "TASK", event.eventType(), title(event.eventType()),
            body(event), "FAILED".equals(status(event.eventType())) ? "HIGH" : "NORMAL",
            "UNREAD", event.subjectId(), null, event.occurredAt(), null, null, 0L)))).then();
    }

    private Mono<UUID> recipient(DurableEvent event) {
        if (event.actorId() != null) return Mono.just(event.actorId());
        try {
            JsonNode payload = mapper.readTree(event.payloadJson());
            JsonNode actor = payload == null ? null : payload.get("actor_id");
            return actor == null || actor.isNull() ? Mono.empty() : Mono.just(UUID.fromString(actor.asText()));
        } catch (JacksonException | IllegalArgumentException ignored) {
            return Mono.empty();
        }
    }

    private boolean isTaskTerminal(String eventType) {
        return SUCCEEDED.equals(eventType) || FAILED.equals(eventType) || TIMED_OUT.equals(eventType);
    }

    private String status(String eventType) {
        return SUCCEEDED.equals(eventType) ? "SUCCEEDED" : TIMED_OUT.equals(eventType) ? "TIMED_OUT" : "FAILED";
    }

    private String title(String eventType) {
        return switch (status(eventType)) {
            case "SUCCEEDED" -> "后台任务已完成";
            case "TIMED_OUT" -> "后台任务已超时";
            default -> "后台任务执行失败";
        };
    }

    private String body(DurableEvent event) {
        return "任务 " + event.subjectId() + " 的执行状态为 " + status(event.eventType()) + "。";
    }
}
