package run.ikaros.storage;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/storage/restore-budget")
public class StorageRestoreBudgetController {
    private final StorageRestoreBudgetService service;
    public StorageRestoreBudgetController(StorageRestoreBudgetService service) { this.service = service; }
    @GetMapping
    public Mono<StorageRestoreBudgetView> get(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId) { return service.get(); }
    @PutMapping
    public Mono<StorageRestoreBudgetView> update(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @Valid @RequestBody StorageRestoreBudgetRequest request) { return service.update(request); }
}
