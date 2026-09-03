package run.ikaros.ingestion;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping({"/api/ingestion/scans"})
public class DiscoveredItemController {
    private final DiscoveredItemService service;
    public DiscoveredItemController(DiscoveredItemService service) { this.service = service; }

    @PostMapping("/{scanId}/items")
    public Mono<DiscoveredItemView> record(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
                                           @PathVariable UUID scanId, @Valid @RequestBody DiscoveredItemRequest request) {
        return service.record(actorId, scanId, request);
    }

    @GetMapping("/{scanId}/items")
    public Mono<List<DiscoveredItemView>> list(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
                                               @PathVariable UUID scanId) {
        return service.list(actorId, scanId);
    }

    @org.springframework.web.bind.annotation.PatchMapping("/items/{itemId}/unavailable")
    public Mono<DiscoveredItemView> unavailable(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
                                                @PathVariable UUID itemId,
                                                @RequestParam(defaultValue = "source unavailable") String reason) {
        return service.markUnavailable(actorId, itemId, reason);
    }
}
