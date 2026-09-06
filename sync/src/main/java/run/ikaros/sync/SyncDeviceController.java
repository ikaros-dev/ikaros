package run.ikaros.sync;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/sync/devices")
public class SyncDeviceController {
    private final DeviceService service;

    public SyncDeviceController(DeviceService service) {
        this.service = service;
    }

    @PostMapping
    public Mono<DeviceView> register(@RequestHeader("X-Ikaros-Actor-Id") UUID userId,
                                     @Valid @RequestBody RegisterDeviceRequest request) {
        return service.register(userId, request);
    }

    @GetMapping
    public Flux<DeviceView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID userId) {
        return service.list(userId);
    }

    @PostMapping("/{deviceId}/revoke")
    public Mono<DeviceView> revoke(@RequestHeader("X-Ikaros-Actor-Id") UUID userId,
                                   @PathVariable UUID deviceId) {
        return service.revoke(userId, deviceId);
    }
}
