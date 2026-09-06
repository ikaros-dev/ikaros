package run.ikaros.sync;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DeviceService {
    Mono<DeviceView> register(UUID userId, RegisterDeviceRequest request);

    Flux<DeviceView> list(UUID userId);

    Mono<DeviceView> revoke(UUID userId, UUID deviceId);
}
