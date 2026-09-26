package run.ikaros.storage;

import run.ikaros.storage.api.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.IfMatchVersion;

@RestController
@RequestMapping("/api/admin/storage-providers")
public class StorageProviderController {
    private final StorageProviderRegistry registry;
    private final StorageProviderCredentialService credentialService;
    private final StorageProviderProbeService probeService;
    private final StorageProviderStatusService statusService;
    private final StorageProviderDeleteService deleteService;

    public StorageProviderController(StorageProviderRegistry registry,
                                     StorageProviderCredentialService credentialService,
                                     StorageProviderProbeService probeService,
                                     StorageProviderStatusService statusService,
                                     StorageProviderDeleteService deleteService) {
        this.registry = registry;
        this.credentialService = credentialService;
        this.probeService = probeService;
        this.statusService = statusService;
        this.deleteService = deleteService;
    }

    @PostMapping
    public Mono<ResponseEntity<StorageProviderView>> register(
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @Valid @RequestBody StorageProviderCreateRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("缺少 Idempotency-Key"));
        }
        String fingerprint = fingerprint(request);
        return registry.registerConfigured(request.providerKey(), request.providerType(), request.displayName(),
                request.tier(), request.credentialRef(), request.capabilities(), request.configuration(),
                idempotencyKey, fingerprint)
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

    @PostMapping("/{providerId}/enable")
    public Mono<StorageProviderView> enable(@PathVariable UUID providerId) {
        return registry.enable(providerId).map(StorageProviderView::from);
    }

    @PostMapping("/{providerId}/disable")
    public Mono<StorageProviderView> disable(@PathVariable UUID providerId) {
        return registry.disable(providerId).map(StorageProviderView::from);
    }

    @DeleteMapping("/{providerId}")
    public Mono<ResponseEntity<Void>> delete(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @RequestHeader(value = "If-Match", required = false) String ifMatch,
        @PathVariable UUID providerId) {
        return deleteService.delete(actorId, providerId, IfMatchVersion.parse(ifMatch))
            .thenReturn(ResponseEntity.noContent().build());
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

    private String fingerprint(StorageProviderCreateRequest request) {
        String body = String.join("\n", request.providerKey(), request.providerType(), request.displayName(),
            request.tier().name(), request.credentialRef() == null ? "secret://default" : request.credentialRef(),
            new java.util.TreeMap<>(request.capabilities()).toString(),
            new java.util.TreeMap<>(request.configuration() == null ? java.util.Map.of() : request.configuration()).toString());
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }
}
