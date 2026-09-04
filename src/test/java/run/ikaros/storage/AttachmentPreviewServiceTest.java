package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.resource.ResourceEntity;
import run.ikaros.resource.ResourceRepository;

class AttachmentPreviewServiceTest {
    private final AttachmentRepository attachments = mock(AttachmentRepository.class);
    private final ResourceRepository resources = mock(ResourceRepository.class);
    private final BlobRepository blobs = mock(BlobRepository.class);
    private final BlobPlacementRepository placements = mock(BlobPlacementRepository.class);
    private final StorageProviderRegistry providers = mock(StorageProviderRegistry.class);
    private final StorageObjectProviderRegistry objects = mock(StorageObjectProviderRegistry.class);
    private final MediaDeliveryBindingRepository bindings = mock(MediaDeliveryBindingRepository.class);
    private final DeliveryProviderRepository deliveryProviders = mock(DeliveryProviderRepository.class);
    private final DeliveryGrantService grants = mock(DeliveryGrantService.class);
    private final DeliveryLeaseService leases = mock(DeliveryLeaseService.class);
    private final DeliveryGrantContractService contracts = mock(DeliveryGrantContractService.class);
    private AttachmentPreviewService service;
    private UUID actorId;
    private UUID attachmentId;
    private UUID blobId;
    private UUID resourceId;
    private UUID storageProviderId;
    private AttachmentEntity attachment;
    private BlobEntity blob;
    private BlobPlacementEntity placement;
    private StorageProvider provider;

    @BeforeEach
    void setUp() {
        service = new AttachmentPreviewService(attachments, resources, blobs, placements, providers, objects,
            bindings, deliveryProviders, grants, leases, contracts);
        actorId = UUID.randomUUID(); attachmentId = UUID.randomUUID(); blobId = UUID.randomUUID();
        resourceId = UUID.randomUUID(); storageProviderId = UUID.randomUUID();
        Instant now = Instant.now();
        attachment = new AttachmentEntity(attachmentId, resourceId, blobId, "video.mp4", AttachmentKind.ORIGINAL, now, null, 0L);
        blob = new BlobEntity(blobId, "sha", 100L, "video/mp4", BlobAvailability.AVAILABLE, now, 0L);
        placement = new BlobPlacementEntity(UUID.randomUUID(), blobId, "oss", StorageTier.HOT, "video.mp4",
            PlacementState.ACTIVE, now, now, 0L);
        provider = new StorageProvider(storageProviderId, "oss", "S3", StorageTier.HOT, StorageProviderStatus.ENABLED,
            "secret://oss", Map.of(), now, now);
        when(attachments.findById(attachmentId)).thenReturn(Mono.just(attachment));
        when(resources.findByIdAndOwnerId(resourceId, actorId)).thenReturn(Mono.just(mock(ResourceEntity.class)));
        when(blobs.findById(blobId)).thenReturn(Mono.just(blob));
        when(placements.findAllByBlobIdOrderByCreatedAtAsc(blobId)).thenReturn(Flux.just(placement));
        when(providers.getByKey("oss")).thenReturn(Mono.just(provider));
    }

    @Test
    void prefersDeliveryBindingOverStorageSignedUrl() {
        MediaDeliveryBindingEntity binding = new MediaDeliveryBindingEntity(UUID.randomUUID(), storageProviderId, "cdn",
            DeliveryBindingOriginType.STORAGE_PROVIDER, DeliveryBindingAuthMode.DELIVERY_GRANT, 1, true,
            DeliveryBindingCacheKeyPolicy.CONTENT_IDENTITY, DeliveryBindingRangePolicy.PASSTHROUGH, true,
            Instant.now(), Instant.now(), 0L);
        DeliveryProviderEntity deliveryProvider = mock(DeliveryProviderEntity.class);
        DeliveryGrantView grant = new DeliveryGrantView(UUID.randomUUID(), attachmentId, "token", "GET",
            Instant.now().plusSeconds(60), null, null, DeliveryGrantRevocationLevel.IMMEDIATE);
        DeliveryLeaseView lease = new DeliveryLeaseView(UUID.randomUUID(), attachmentId, blobId,
            Instant.now().plusSeconds(60), Instant.now(), true);
        DeliveryGrantContractView contract = new DeliveryGrantContractView(grant.id(), attachmentId, lease.id(),
            UUID.randomUUID(), "GET", "/api/attachments/" + attachmentId + "/content?delivery_grant=token",
            grant.expiresAt(), true, "video/mp4", 100L, DeliveryGrantRevocationLevel.IMMEDIATE);
        when(bindings.findAllByStorageProviderIdOrderByPriorityAsc(storageProviderId)).thenReturn(Flux.just(binding));
        when(deliveryProviders.findByProviderKey("cdn")).thenReturn(Mono.just(deliveryProvider));
        when(deliveryProvider.enabled()).thenReturn(true);
        when(deliveryProvider.healthStatus()).thenReturn(DeliveryProviderHealthStatus.HEALTHY);
        when(grants.issue(eq(actorId), eq(attachmentId), any())).thenReturn(Mono.just(grant));
        when(leases.create(eq(actorId), eq(attachmentId), any())).thenReturn(Mono.just(lease));
        when(contracts.contract(attachmentId, grant, lease)).thenReturn(Mono.just(contract));

        StepVerifier.create(service.issue(actorId, attachmentId))
            .assertNext(result -> assertThat(result.url()).contains("delivery_grant=token"))
            .verifyComplete();
        verifyNoInteractions(objects);
    }

    @Test
    void fallsBackToStorageSignedUrlWhenNoDeliveryBindingExists() {
        when(bindings.findAllByStorageProviderIdOrderByPriorityAsc(storageProviderId)).thenReturn(Flux.empty());
        StorageReadIntent intent = new StorageReadIntent("GET", "https://storage.example/video.mp4?signature=x",
            Instant.now().plusSeconds(60));
        when(objects.createReadIntent(provider, "video.mp4")).thenReturn(Mono.just(intent));

        StepVerifier.create(service.issue(actorId, attachmentId))
            .assertNext(result -> {
                assertThat(result.url()).startsWith("https://storage.example");
                assertThat(result.rangeSupported()).isTrue();
            })
            .verifyComplete();
        verifyNoInteractions(grants, leases, contracts);
    }
}
