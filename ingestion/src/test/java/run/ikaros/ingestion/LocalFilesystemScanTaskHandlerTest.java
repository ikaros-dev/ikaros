package run.ikaros.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.springframework.test.util.ReflectionTestUtils;
import run.ikaros.common.ConflictException;
import run.ikaros.operations.api.BackgroundTask;
import run.ikaros.operations.api.BackgroundTaskDispatcher;
import run.ikaros.operations.api.BackgroundTaskService;
import run.ikaros.operations.api.TaskStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class LocalFilesystemScanTaskHandlerTest {
    @TempDir
    Path root;

    @Test
    @ResourceLock(Resources.SYSTEM_PROPERTIES)
    void unixSensitiveDirectoryPolicyAllowsOrdinaryAbsolutePaths() {
        LocalFilesystemScanTaskHandler handler = new LocalFilesystemScanTaskHandler(
            null, null, null, null, null, null, 100);
        String originalOs = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Linux");
            for (String path : List.of("/tmp/media", "/home/user/media", "/etc-media", "/var/library")) {
                Boolean sensitive = ReflectionTestUtils.invokeMethod(handler, "isSensitiveRoot", Path.of(path));
                assertFalse(sensitive, path + " must be allowed as a scan root");
            }
            for (String path : List.of("/etc", "/etc/ssl", "/proc", "/sys", "/dev", "/run", "/root",
                "/boot", "/usr", "/bin", "/sbin", "/var/lib", "/var/lib/private")) {
                Boolean sensitive = ReflectionTestUtils.invokeMethod(handler, "isSensitiveRoot", Path.of(path));
                assertTrue(sensitive, path + " must remain blocked");
            }
        } finally {
            if (originalOs == null) System.clearProperty("os.name");
            else System.setProperty("os.name", originalOs);
        }
    }

    @Test
    void rejectsFilesystemRootBeforeScanning() throws Exception {
        LocalFilesystemScanTaskHandler handler = new LocalFilesystemScanTaskHandler(
            null, null, null, null, null, null, 100);
        Mono<?> resolution = ReflectionTestUtils.invokeMethod(handler, "resolveRoot",
            root.toRealPath().getRoot().toString());

        StepVerifier.create(resolution)
            .expectErrorSatisfies(error -> {
                assertTrue(error instanceof ConflictException);
                assertEquals("本地来源根目录不可扫描", error.getMessage());
            })
            .verify();
    }

    @Test
    void discoversFilesAndCreatesCandidatesWithoutReadingUnsupportedFiles() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID scanId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        Instant now = Instant.now();
        Files.writeString(root.resolve("movie.mkv"), "not read by scanner");
        Files.writeString(root.resolve("notes.csv"), "not a supported resource");
        Path sensitive = Files.createDirectories(root.resolve(".ssh"));
        Files.writeString(sensitive.resolve("id_rsa"), "never scan");

        BackgroundTask task = new BackgroundTask(taskId, "ingestion.scan", TaskStatus.RUNNING,
            Map.of("actor_id", ownerId.toString(), "source_id", sourceId.toString(), "scan_run_id", scanId.toString()),
            null, now, null, "worker", UUID.randomUUID(), now.plusSeconds(60), 1, null,
            Map.of(), Map.of(), now, now, null);
        ScanRunService scans = mock(ScanRunService.class);
        IngestionSourceRepository sources = mock(IngestionSourceRepository.class);
        DiscoveredItemService discovered = mock(DiscoveredItemService.class);
        IngestionCandidateService candidates = mock(IngestionCandidateService.class);
        BackgroundTaskService tasks = mock(BackgroundTaskService.class);
        when(scans.get(ownerId, scanId)).thenReturn(Mono.just(new ScanRunView(scanId, sourceId, "console", ownerId,
            ScanRunStatus.PENDING, null, 0, 0, 0, null, taskId, null, null, now)));
        when(sources.findByIdAndOwnerId(sourceId, ownerId)).thenReturn(Mono.just(new IngestionSourceEntity(sourceId,
            ownerId, "LOCAL_FILESYSTEM", "Pictures", root.toString(), null, "{}", "ENABLED", null, "UNKNOWN",
            now, now, 0L)));
        when(tasks.get(taskId)).thenReturn(Mono.just(task));
        when(scans.checkpoint(eq(scanId), any(), any(Long.class), any(Long.class), any(Long.class), eq(null)))
            .thenReturn(Mono.just(new ScanRunView(scanId, sourceId, "console", ownerId,
                ScanRunStatus.RUNNING, "starting", 0, 0, 0, null, taskId, now, null, now)));
        when(scans.finish(eq(scanId), eq(ScanRunStatus.SUCCEEDED), any(), any(Long.class), any(Long.class),
            any(Long.class), eq(null))).thenReturn(Mono.just(new ScanRunView(scanId, sourceId, "console", ownerId,
                ScanRunStatus.SUCCEEDED, "completed", 2, 1, 1, null, taskId, now, now, now)));
        when(discovered.record(eq(ownerId), eq(scanId), any())).thenAnswer(invocation -> {
            DiscoveredItemRequest request = invocation.getArgument(2);
            return Mono.just(new DiscoveredItemView(UUID.randomUUID(), sourceId, scanId, request.relativeKey(),
                request.sizeBytes(), request.modifiedAt(), request.etag(), request.mediaType(), request.availability(),
                request.scanGeneration()));
        });
        when(candidates.create(eq(ownerId), eq(scanId), any())).thenAnswer(invocation -> {
            CreateCandidateRequest request = invocation.getArgument(2);
            return Mono.just(new IngestionCandidateView(UUID.randomUUID(), scanId, sourceId,
                request.suggestedResourceType(), request.titleHint(), request.externalIdHint(), request.confidence(),
                request.fingerprint(), CandidateStatus.NEW, now));
        });

        LocalFilesystemScanTaskHandler handler = new LocalFilesystemScanTaskHandler(mock(BackgroundTaskDispatcher.class),
            tasks, sources, scans, discovered, candidates, 100);

        StepVerifier.create(handler.handle(task))
            .expectNextMatches(result -> result.get("scan_run_id").equals(scanId.toString())
                && result.get("discovered_count").equals(2L)
                && result.get("candidate_count").equals(1L)
                && result.get("skipped_count").equals(1L))
            .verifyComplete();

        verify(discovered, org.mockito.Mockito.times(2)).record(eq(ownerId), eq(scanId), any());
        verify(candidates).create(eq(ownerId), eq(scanId), any());
        verify(scans).finish(scanId, ScanRunStatus.SUCCEEDED, "completed", 2, 1, 1, null);
    }
}
