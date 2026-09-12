package run.ikaros.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.sync.api.DeviceTrustQuery;

@ExtendWith(MockitoExtension.class)
class PersistentDownloadPauseResumeTest {
    @Mock DownloadIntentRepository repository;
    @Mock DeviceTrustQuery devices;

    @Test
    void pausesDownloadingTask() {
        UUID userId = UUID.randomUUID();
        UUID intentId = UUID.randomUUID();
        DownloadIntentEntity current = task(intentId, userId, DownloadState.DOWNLOADING);
        when(repository.findById(intentId)).thenReturn(Mono.just(current));
        when(repository.save(any(DownloadIntentEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(new PersistentDownloadService(repository, devices).updateState(
                userId, intentId, new UpdateDownloadStateRequest(DownloadState.PAUSED, null)))
            .assertNext(view -> assertThat(view.state()).isEqualTo(DownloadState.PAUSED))
            .verifyComplete();
    }

    @Test
    void resumesPausedTask() {
        UUID userId = UUID.randomUUID();
        UUID intentId = UUID.randomUUID();
        DownloadIntentEntity current = task(intentId, userId, DownloadState.PAUSED);
        when(repository.findById(intentId)).thenReturn(Mono.just(current));
        when(repository.save(any(DownloadIntentEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(new PersistentDownloadService(repository, devices).updateState(
                userId, intentId, new UpdateDownloadStateRequest(DownloadState.DOWNLOADING, null)))
            .assertNext(view -> assertThat(view.state()).isEqualTo(DownloadState.DOWNLOADING))
            .verifyComplete();
    }

    @Test
    void cannotResumeRemovedTask() {
        UUID userId = UUID.randomUUID();
        UUID intentId = UUID.randomUUID();
        when(repository.findById(intentId)).thenReturn(Mono.just(task(intentId, userId, DownloadState.REMOVED)));

        StepVerifier.create(new PersistentDownloadService(repository, devices).updateState(
                userId, intentId, new UpdateDownloadStateRequest(DownloadState.DOWNLOADING, null)))
            .expectErrorMessage("Download 状态迁移不合法")
            .verify();
    }

    private static DownloadIntentEntity task(UUID id, UUID userId, DownloadState state) {
        Instant now = Instant.now();
        return new DownloadIntentEntity(id, userId, UUID.randomUUID(), UUID.randomUUID(), null,
            OfflineCopyKind.DOWNLOAD, state, null, 1, now, now, 0L);
    }
}
