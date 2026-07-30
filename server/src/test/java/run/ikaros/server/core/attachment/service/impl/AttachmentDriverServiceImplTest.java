package run.ikaros.server.core.attachment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.core.attachment.extension.LocalDiskAttachmentDriverFetcher;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentDriverRepository;
import run.ikaros.server.store.repository.AttachmentRepository;

/** 附件驱动增量刷新服务测试. */
class AttachmentDriverServiceImplTest {
    /** 附件驱动仓库. */
    @Mock
    private AttachmentDriverRepository driverRepository;
    /** 附件仓库. */
    @Mock
    private AttachmentRepository attachmentRepository;
    /** 应用事件发布器. */
    @Mock
    private ApplicationEventPublisher eventPublisher;
    /** 附件服务. */
    @Mock
    private AttachmentService attachmentService;
    /** 响应式数据库模板. */
    @Mock
    private R2dbcEntityTemplate template;
    /** 扩展组件查找器. */
    @Mock
    private ExtensionComponentsFinder extensionComponentsFinder;
    /** 附件驱动扫描器. */
    @Mock
    private AttachmentDriverFetcher fetcher;
    /** 被测试的附件驱动服务. */
    private AttachmentDriverServiceImpl service;
    /** 驱动ID. */
    private UUID driverId;
    /** 当前目录附件ID. */
    private UUID parentId;
    /** 当前目录磁盘路径. */
    private String remotePath;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AttachmentDriverServiceImpl(driverRepository, attachmentRepository,
            eventPublisher, attachmentService, template, extensionComponentsFinder);
        driverId = UUID.randomUUID();
        parentId = UUID.randomUUID();
        remotePath = "D:/media";
        Attachment parentAttachment = Attachment.builder()
            .id(parentId)
            .driverId(driverId)
            .type(AttachmentType.Driver_Directory)
            .fsPath(remotePath)
            .build();
        AttachmentDriverEntity driver = AttachmentDriverEntity.builder()
            .id(driverId)
            .type(AttachmentDriverType.LOCAL)
            .name(LocalDiskAttachmentDriverFetcher.LOCAL_DISK_DRIVER_NAME)
            .build();
        when(attachmentService.findById(parentId)).thenReturn(Mono.just(parentAttachment));
        when(driverRepository.findById(driverId)).thenReturn(Mono.just(driver));
        when(extensionComponentsFinder.getExtensions(AttachmentDriverFetcher.class))
            .thenReturn(List.of(fetcher));
        when(fetcher.getDriverType()).thenReturn(AttachmentDriverType.LOCAL);
        when(fetcher.getDriverName())
            .thenReturn(LocalDiskAttachmentDriverFetcher.LOCAL_DISK_DRIVER_NAME);
        when(attachmentService.save(any(Attachment.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(attachmentService.removeByIdOnlyRecords(any(UUID.class))).thenReturn(Mono.empty());
    }

    @Test
    void refreshCalculatesSha1ForEveryFileOnFirstScan() {
        Attachment firstFile = scannedFile("D:/media/first.mkv", 100L,
            LocalDateTime.of(2026, 7, 30, 12, 0));
        Attachment secondFile = scannedFile("D:/media/second.mkv", 200L,
            LocalDateTime.of(2026, 7, 30, 12, 1));
        when(fetcher.getChildren(driverId, parentId, remotePath))
            .thenReturn(Flux.just(firstFile, secondFile));
        when(attachmentRepository.findAllByParentIdAndDriverId(parentId, driverId))
            .thenReturn(Flux.empty());
        when(fetcher.calculateSha1(any(Attachment.class)))
            .thenAnswer(invocation -> Mono.just(
                invocation.<Attachment>getArgument(0).setSha1("sha1")));

        StepVerifier.create(service.refresh(parentId)).verifyComplete();

        verify(fetcher, times(2)).calculateSha1(any(Attachment.class));
        verify(attachmentService, times(2)).save(any(Attachment.class));
    }

    @Test
    void refreshSkipsUnchangedFiles() {
        LocalDateTime modifiedTime = LocalDateTime.of(2026, 7, 30, 12, 0);
        Attachment scannedFile = scannedFile("D:/media/unchanged.mkv", 100L, modifiedTime);
        AttachmentEntity storedFile = storedFile(scannedFile, modifiedTime, "stored-sha1");
        when(fetcher.getChildren(driverId, parentId, remotePath))
            .thenReturn(Flux.just(scannedFile));
        when(attachmentRepository.findAllByParentIdAndDriverId(parentId, driverId))
            .thenReturn(Flux.just(storedFile));

        StepVerifier.create(service.refresh(parentId)).verifyComplete();

        verify(fetcher, never()).calculateSha1(any(Attachment.class));
        verify(attachmentService, never()).save(any(Attachment.class));
        verify(attachmentService, never()).removeByIdOnlyRecords(any(UUID.class));
    }

    @Test
    void refreshOnlyHashesChangedFilesAndRemovesMissingFiles() {
        LocalDateTime oldModifiedTime = LocalDateTime.of(2026, 7, 30, 12, 0);
        LocalDateTime newModifiedTime = oldModifiedTime.plusMinutes(1);
        Attachment changedFile = scannedFile("D:/media/changed.mkv", 101L, newModifiedTime);
        AttachmentEntity storedChangedFile = storedFile(changedFile, oldModifiedTime, "old-sha1")
            .setSize(100L);
        AttachmentEntity missingFile = storedFile(
            scannedFile("D:/media/missing.mkv", 200L, oldModifiedTime),
            oldModifiedTime, "missing-sha1");
        when(fetcher.getChildren(driverId, parentId, remotePath))
            .thenReturn(Flux.just(changedFile));
        when(attachmentRepository.findAllByParentIdAndDriverId(parentId, driverId))
            .thenReturn(Flux.just(storedChangedFile, missingFile));
        when(fetcher.calculateSha1(changedFile))
            .thenReturn(Mono.just(changedFile.setSha1("new-sha1")));

        StepVerifier.create(service.refresh(parentId)).verifyComplete();

        ArgumentCaptor<Attachment> attachmentCaptor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentService).save(attachmentCaptor.capture());
        assertThat(attachmentCaptor.getValue().getId()).isEqualTo(storedChangedFile.getId());
        assertThat(attachmentCaptor.getValue().getSha1()).isEqualTo("new-sha1");
        verify(attachmentService).removeByIdOnlyRecords(missingFile.getId());
    }

    @Test
    void refreshMergesConcurrentRequestsForSameDirectory() {
        Attachment file = scannedFile("D:/media/concurrent.mkv", 100L,
            LocalDateTime.of(2026, 7, 30, 12, 0));
        when(fetcher.getChildren(driverId, parentId, remotePath))
            .thenReturn(Flux.just(file).delayElements(Duration.ofMillis(50)));
        when(attachmentRepository.findAllByParentIdAndDriverId(parentId, driverId))
            .thenReturn(Flux.empty());
        when(fetcher.calculateSha1(file)).thenReturn(Mono.just(file.setSha1("sha1")));

        StepVerifier.create(Flux.merge(service.refresh(parentId), service.refresh(parentId)))
            .verifyComplete();

        verify(fetcher).getChildren(driverId, parentId, remotePath);
        verify(fetcher).calculateSha1(file);
        verify(attachmentService).save(file);
    }

    private Attachment scannedFile(String fsPath, Long size, LocalDateTime modifiedTime) {
        return Attachment.builder()
            .parentId(parentId)
            .driverId(driverId)
            .type(AttachmentType.Driver_File)
            .name(fsPath.substring(fsPath.lastIndexOf('/') + 1))
            .path(fsPath)
            .url(fsPath)
            .fsPath(fsPath)
            .size(size)
            .modifiedTime(modifiedTime)
            .deleted(false)
            .build();
    }

    private AttachmentEntity storedFile(Attachment attachment, LocalDateTime modifiedTime,
                                        String sha1) {
        return AttachmentEntity.builder()
            .id(UUID.randomUUID())
            .parentId(parentId)
            .driverId(driverId)
            .type(attachment.getType())
            .name(attachment.getName())
            .path(attachment.getPath())
            .url(attachment.getUrl())
            .fsPath(attachment.getFsPath())
            .size(attachment.getSize())
            .modifiedTime(modifiedTime)
            .sha1(sha1)
            .deleted(false)
            .build();
    }
}
