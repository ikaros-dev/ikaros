package run.ikaros.operations.notification;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import run.ikaros.common.PageResponse;

@RestController
@RequestMapping("/api/communications/notifications")
public class NotificationController {
    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public Mono<PageResponse<NotificationView>> list(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String source,
                                                     @RequestParam(required = false) String priority,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return notifications.search(actorId, status, source, priority, page, size);
    }

    @PostMapping("/{notificationId}/actions/read")
    public Mono<NotificationView> markRead(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
                                            @PathVariable UUID notificationId) {
        return notifications.markRead(actorId, notificationId);
    }

}
