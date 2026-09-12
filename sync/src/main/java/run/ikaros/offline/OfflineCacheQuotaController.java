package run.ikaros.offline;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/offline/cache/quota")
public class OfflineCacheQuotaController {
    private final OfflineCacheQuotaService service;
    public OfflineCacheQuotaController(OfflineCacheQuotaService service) { this.service = service; }
    @GetMapping public Mono<OfflineCacheQuotaView> get(@RequestHeader("X-Ikaros-Actor-Id") UUID user,
        @RequestParam UUID deviceId) { return service.get(user, deviceId); }
    @PutMapping public Mono<OfflineCacheQuotaView> set(@RequestHeader("X-Ikaros-Actor-Id") UUID user,
        @Valid @RequestBody SetOfflineCacheQuotaRequest request) { return service.set(user, request); }
}
