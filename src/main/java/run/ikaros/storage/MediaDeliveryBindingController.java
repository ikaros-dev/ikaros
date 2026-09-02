package run.ikaros.storage;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/storage/providers/{providerId}/delivery-bindings")
public class MediaDeliveryBindingController {
    private final MediaDeliveryBindingService service;
    public MediaDeliveryBindingController(MediaDeliveryBindingService service) { this.service = service; }
    @PostMapping
    public Mono<MediaDeliveryBindingView> create(@PathVariable UUID providerId,
        @Valid @RequestBody MediaDeliveryBindingRequest request) { return service.create(providerId, request); }
    @GetMapping
    public Flux<MediaDeliveryBindingView> list(@PathVariable UUID providerId) { return service.list(providerId); }
    @PutMapping("/{bindingId}")
    public Mono<MediaDeliveryBindingView> update(@PathVariable UUID bindingId,
        @Valid @RequestBody MediaDeliveryBindingRequest request) { return service.update(bindingId, request); }
    @DeleteMapping("/{bindingId}")
    public Mono<Void> delete(@PathVariable UUID bindingId) { return service.delete(bindingId); }
}
