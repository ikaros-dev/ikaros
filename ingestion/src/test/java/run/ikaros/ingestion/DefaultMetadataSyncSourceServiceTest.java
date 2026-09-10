package run.ikaros.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.operations.api.AuditService;

class DefaultMetadataSyncSourceServiceTest {
    private final MetadataSyncSourceRepository repository = org.mockito.Mockito.mock(MetadataSyncSourceRepository.class);
    private final AuditService audit = org.mockito.Mockito.mock(AuditService.class);
    private final DefaultMetadataSyncSourceService service =
        new DefaultMetadataSyncSourceService(repository, audit, null);

    @Test
    void createsOwnerScopedSyncSourceWithoutExposingCredential() {
        UUID ownerId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(repository.save(any(MetadataSyncSourceEntity.class))).thenAnswer(invocation -> {
            MetadataSyncSourceEntity source = invocation.getArgument(0);
            return Mono.just(new MetadataSyncSourceEntity(id, source.ownerId(), source.providerKey(),
                source.displayName(), source.credentialReference(), source.refreshSchedule(), source.status(),
                Instant.now(), Instant.now(), 0L));
        });
        when(audit.record(ownerId, "metadata.sync-source.create", "METADATA_SYNC_SOURCE", id, "{}"))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.create(ownerId, new CreateMetadataSyncSourceRequest(
                "tmdb", "TMDB", "secret://env/TMDB_TOKEN", "daily")))
            .assertNext(view -> {
                assertThat(view.id()).isEqualTo(id);
                assertThat(view.credentialConfigured()).isTrue();
                assertThat(view.refreshSchedule()).isEqualTo("DAILY");
            })
            .verifyComplete();
    }

    @Test
    void rejectsPlaintextCredentialAndInvalidScheduleBeforePersistence() {
        UUID ownerId = UUID.randomUUID();
        StepVerifier.create(service.create(ownerId, new CreateMetadataSyncSourceRequest(
                "tmdb", "TMDB", "plaintext-token", "weekly")))
            .expectErrorSatisfies(error -> assertThat(error).hasMessage("credential reference 必须使用 secret:// URI"))
            .verify();
        verify(repository, never()).save(any());
    }
}
