package run.ikaros.storage;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping({"/api/storage/providers", "/api/admin/storage-providers"})
public class StorageProviderController {
    private final StorageProviderRegistry registry;

    public StorageProviderController(StorageProviderRegistry registry) {
        this.registry = registry;
    }

    @PostMapping
    public Mono<ResponseEntity<StorageProvider>> register(@Valid @RequestBody RegisterStorageProviderRequest request) {
        return registry.register(request.providerKey(), request.providerType(), request.tier(),
                request.secretReference(), request.metadata(), request.accessKeyId(), request.secretAccessKey(), request.sessionToken())
            .map(provider -> ResponseEntity.created(URI.create("/api/storage/providers/" + provider.id()))
                .body(provider));
    }

    @GetMapping
    public Flux<StorageProvider> list() {
        return registry.list();
    }

    @GetMapping("/{providerId}")
    public Mono<StorageProvider> get(@PathVariable UUID providerId) {
        return registry.get(providerId);
    }

    @PostMapping("/{providerId}/enable")
    public Mono<StorageProvider> enable(@PathVariable UUID providerId) {
        return registry.enable(providerId);
    }

    @DeleteMapping("/{providerId}")
    public Mono<ResponseEntity<Void>> disable(@PathVariable UUID providerId) {
        return registry.disable(providerId).thenReturn(ResponseEntity.noContent().build());
    }
}
