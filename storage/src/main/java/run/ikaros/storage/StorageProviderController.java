package run.ikaros.storage;

import run.ikaros.storage.api.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/storage-providers")
public class StorageProviderController {
    private final StorageProviderRegistry registry;
    private final StorageProviderCredentialService credentialService;
    private final StorageProviderProbeService probeService;
    private final StorageProviderStatusService statusService;

    public StorageProviderController(StorageProviderRegistry registry,
                                     StorageProviderCredentialService credentialService,
                                     StorageProviderProbeService probeService,
                                     StorageProviderStatusService statusService) {
        this.registry = registry;
        this.credentialService = credentialService;
        this.probeService = probeService;
        this.statusService = statusService;
    }

    @PostMapping
    public Mono<ResponseEntity<StorageProviderView>> register(@Valid @RequestBody RegisterStorageProviderRequest request) {
        return registry.register(request.providerKey(), request.providerType(), request.tier(),
                request.secretReference(), request.metadata(), request.accessKeyId(), request.secretAccessKey(), request.sessionToken())
            .map(provider -> ResponseEntity.created(URI.create("/api/admin/storage-providers/" + provider.id()))
                .body(StorageProviderView.from(provider)));
    }

    @GetMapping
    public Flux<StorageProviderView> list() {
        return registry.list().map(StorageProviderView::from);
    }

    @GetMapping("/{providerId}")
    public Mono<StorageProviderView> get(@PathVariable UUID providerId) {
        return registry.get(providerId).map(StorageProviderView::from);
    }

    @PostMapping("/{providerId}/probe")
    public Mono<StorageProviderProbeView> probe(@PathVariable UUID providerId) {
        return probeService.probe(providerId).map(StorageProviderProbeView::from);
    }

    @GetMapping("/{providerId}/status")
    public Mono<StorageProviderStatusResponse> status(@PathVariable UUID providerId) {
        return statusService.get(providerId).map(StorageProviderStatusResponse::from);
    }

    @PostMapping("/{providerId}/credentials")
    public Mono<StorageProviderProbeView> replaceCredentials(@PathVariable UUID providerId,
                                                          @Valid @RequestBody ReplaceStorageProviderCredentialsRequest request) {
        return credentialService.replace(providerId, request)
            .then(probeService.probe(providerId)).map(StorageProviderProbeView::from);
    }
}
