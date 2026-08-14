package run.ikaros.server.core.attachment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.core.attachment.service.AttachmentService;

/**
 * 默认附件 SHA-1 后台计算服务测试.
 */
@org.jspecify.annotations.NullUnmarked
class DefaultAttachmentSha1ServiceTest {
    /** 附件服务. */
    @Mock
    private AttachmentService attachmentService;
    /** 附件驱动读取器. */
    @Mock
    private AttachmentDriverFetcher fetcher;
    /** 被测试的 SHA-1 服务. */
    private DefaultAttachmentSha1Service service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DefaultAttachmentSha1Service(
            Schedulers.immediate(), attachmentService);
    }

    @Test
    void calculateAsyncSavesSha1ForUnchangedFile() {
        Attachment snapshot = file("D:/media/episode.mkv");
        Attachment currentAttachment = file(snapshot.getFsPath())
            .setId(snapshot.getId())
            .setDriverId(snapshot.getDriverId());
        when(fetcher.calculateSha1(snapshot))
            .thenReturn(Mono.just(snapshot.setSha1("sha1")));
        when(attachmentService.findById(snapshot.getId()))
            .thenReturn(Mono.just(currentAttachment));
        when(attachmentService.save(currentAttachment))
            .thenReturn(Mono.just(currentAttachment));

        service.calculateAsync(fetcher, List.of(snapshot));

        assertThat(currentAttachment.getSha1()).isEqualTo("sha1");
        verify(attachmentService).save(currentAttachment);
    }

    @Test
    void calculateAndSaveSkipsFileChangedAfterScan() {
        Attachment snapshot = file("D:/media/episode.mkv");
        Attachment currentAttachment = file(snapshot.getFsPath())
            .setId(snapshot.getId())
            .setDriverId(snapshot.getDriverId())
            .setSize(200L);
        when(fetcher.calculateSha1(snapshot))
            .thenReturn(Mono.just(snapshot.setSha1("stale-sha1")));
        when(attachmentService.findById(snapshot.getId()))
            .thenReturn(Mono.just(currentAttachment));

        StepVerifier.create(service.calculateAndSave(fetcher, snapshot)).verifyComplete();

        verify(attachmentService, never()).save(currentAttachment);
    }

    private Attachment file(String fsPath) {
        return Attachment.builder()
            .id(UUID.randomUUID())
            .driverId(UUID.randomUUID())
            .type(AttachmentType.Driver_File)
            .fsPath(fsPath)
            .size(100L)
            .modifiedTime(LocalDateTime.of(2026, 7, 30, 12, 0))
            .deleted(false)
            .build();
    }
}
