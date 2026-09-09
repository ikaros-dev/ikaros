package run.ikaros.operations.notification;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/communications/notification-preferences")
public class NotificationPreferenceController {
    private final NotificationPreferenceService preferences;

    public NotificationPreferenceController(NotificationPreferenceService preferences) {
        this.preferences = preferences;
    }

    @GetMapping
    public Mono<NotificationPreferenceView> get(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId) {
        return preferences.get(actorId);
    }

    @PutMapping
    public Mono<NotificationPreferenceView> update(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
                                                   @RequestBody NotificationPreferenceRequest request) {
        return preferences.update(actorId, request);
    }
}
