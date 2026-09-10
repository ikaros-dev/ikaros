package run.ikaros.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import run.ikaros.operations.api.AuditService;
import run.ikaros.operations.api.BackgroundTask;
import run.ikaros.operations.api.BackgroundTaskService;
import run.ikaros.operations.api.TaskStatus;

class DefaultScanRunServiceTest {
    @Test
    void startsScanAsPendingBackgroundTaskAndAuditsIt() {
        UUID ownerId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        Instant now = Instant.now();
        IngestionSourceEntity source = new IngestionSourceEntity(sourceId, ownerId, "LOCAL_FILESYSTEM", "Media",
            "C:/media", "secret://media", "{}", IngestionSourceStatus.ENABLED.name(), null, "UNKNOWN", now, now, 0L);
        BackgroundTask task = new BackgroundTask(taskId, "ingestion.scan", TaskStatus.PENDING, Map.of(), null,
            now, null, null, null, null, 0, null, Map.of(), Map.of(), now, now, null);
        IngestionSourceRepository sources = mock(IngestionSourceRepository.class);
        ScanRunRepository runs = mock(ScanRunRepository.class);
        BackgroundTaskService tasks = mock(BackgroundTaskService.class);
        AuditService audit = mock(AuditService.class);
        when(sources.findByIdAndOwnerId(sourceId, ownerId)).thenReturn(Mono.just(source));
        when(tasks.submit(eq("ingestion.scan"), any(), eq("ingestion.scan:" + sourceId))).thenReturn(Mono.just(task));
        when(runs.save(any(ScanRunEntity.class))).thenAnswer(invocation ->
            Mono.just(new ScanRunEntity(runId, sourceId, ownerId, "console", ownerId, "PENDING", null,
                0, 0, 0, null, taskId, now, null, now, 0L)));
        when(audit.record(eq(ownerId), eq("ingestion.scan.start"), eq("INGESTION_SCAN"), eq(runId), eq("{}")))
            .thenReturn(Mono.empty());

        ScanRunView result = new DefaultScanRunService(sources, runs, tasks, audit)
            .start(ownerId, sourceId, new StartScanRequest("console")).block();

        assertEquals(runId, result.id());
        assertEquals(ScanRunStatus.PENDING, result.status());
        verify(audit).record(ownerId, "ingestion.scan.start", "INGESTION_SCAN", runId, "{}");
    }

    @Test
    void rejectsDisabledSourceBeforeSubmittingTask() {
        UUID ownerId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        Instant now = Instant.now();
        IngestionSourceRepository sources = mock(IngestionSourceRepository.class);
        BackgroundTaskService tasks = mock(BackgroundTaskService.class);
        IngestionSourceEntity source = new IngestionSourceEntity(sourceId, ownerId, "LOCAL_FILESYSTEM", "Media",
            "C:/media", null, "{}", IngestionSourceStatus.DISABLED.name(), null, "UNKNOWN", now, now, 0L);
        when(sources.findByIdAndOwnerId(sourceId, ownerId)).thenReturn(Mono.just(source));

        RuntimeException error = assertThrows(RuntimeException.class, () -> new DefaultScanRunService(
            sources, mock(ScanRunRepository.class), tasks, mock(AuditService.class))
            .start(ownerId, sourceId, new StartScanRequest("console")).block());

        assertEquals("Source 不存在或当前未启用", error.getMessage());
        verify(tasks, org.mockito.Mockito.never()).submit(any(), any(), any());
    }

    @Test
    void rejectsNegativeCheckpointCountersBeforeRepositoryAccess() {
        ScanRunRepository runs = mock(ScanRunRepository.class);

        RuntimeException error = assertThrows(RuntimeException.class, () -> new DefaultScanRunService(
            mock(IngestionSourceRepository.class), runs, mock(BackgroundTaskService.class), mock(AuditService.class))
            .checkpoint(UUID.randomUUID(), "file.mkv", -1, 0, 0, null).block());

        assertEquals("扫描统计不能为负数", error.getMessage());
        verify(runs, org.mockito.Mockito.never()).findById(any(UUID.class));
    }
}
