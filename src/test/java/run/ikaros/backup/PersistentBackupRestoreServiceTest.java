package run.ikaros.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.common.ConflictException;

class PersistentBackupRestoreServiceTest {
    private final RestorePointRepository repository = mock(RestorePointRepository.class);
    private final PersistentBackupRestoreService service = new PersistentBackupRestoreService(repository);
    private final UUID id = UUID.randomUUID();

    @Test
    void publishingAlreadyPublishedRestorePointIsIdempotent() {
        RestorePointEntity point = point(RestorePointState.PUBLISHED, VerificationStatus.PASSED);
        when(repository.findById(id)).thenReturn(Mono.just(point));

        StepVerifier.create(service.publish(id))
            .assertNext(view -> assertEquals(RestorePointState.PUBLISHED, view.state()))
            .verifyComplete();
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void publishingUnverifiedRestorePointIsRejected() {
        when(repository.findById(id)).thenReturn(Mono.just(point(RestorePointState.PREPARING,
            VerificationStatus.PASSED)));

        StepVerifier.create(service.publish(id))
            .expectError(ConflictException.class)
            .verify();
    }

    @Test
    void verificationRejectsImpossibleObjectCounts() {
        VerifyRestorePointRequest request = new VerifyRestorePointRequest(
            VerificationLevel.CONTENT_FULL, VerificationStatus.PASSED, null, 2, 3);

        StepVerifier.create(service.verify(id, request))
            .expectError(IllegalArgumentException.class)
            .verify();
        verify(repository, never()).findById(id);
    }

    private RestorePointEntity point(RestorePointState state, VerificationStatus status) {
        return new RestorePointEntity(id, "v1", "instance", "schema", "digest", state,
            VerificationLevel.CONTENT_FULL, status, null, 1, 0, Instant.now(),
            state == RestorePointState.PUBLISHED ? Instant.now() : null, 0L);
    }
}
