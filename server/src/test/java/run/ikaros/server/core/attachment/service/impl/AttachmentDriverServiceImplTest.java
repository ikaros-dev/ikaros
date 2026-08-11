package run.ikaros.server.core.attachment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentConst;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.core.attachment.exception.AttachmentNotFoundException;
import run.ikaros.api.core.attachment.exception.NoAvailableAttDriverFetcherException;
import run.ikaros.api.core.media.MediaFileDetectionResult;
import run.ikaros.api.core.media.MediaFileFormat;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.attachment.event.AttachmentDriverEnableEvent;
import run.ikaros.server.core.attachment.extension.LocalDiskAttachmentDriverFetcher;
import run.ikaros.server.core.attachment.service.AttachmentContentInspectionService;
import run.ikaros.server.core.attachment.service.AttachmentDriverMountService;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.core.attachment.service.AttachmentSha1Service;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentDriverRepository;
import run.ikaros.server.store.repository.AttachmentRepository;

/**
 * 附件驱动增量刷新服务测试.
 */
class AttachmentDriverServiceImplTest {
    /**
     * 附件驱动仓库.
     */
    @Mock
    private AttachmentDriverRepository driverRepository;
    /**
     * 附件仓库.
     */
    @Mock
    private AttachmentRepository attachmentRepository;
    /**
     * 应用事件发布器.
     */
    @Mock
    private ApplicationEventPublisher eventPublisher;
    /**
     * 附件服务.
     */
    @Mock
    private AttachmentService attachmentService;
    /**
     * 附件驱动挂载服务.
     */
    @Mock
    private AttachmentDriverMountService mountService;
    /**
     * 附件 SHA-1 后台计算服务.
     */
    @Mock
    private AttachmentSha1Service attachmentSha1Service;
    /**
     * 响应式数据库模板.
     */
    @Mock
    private R2dbcEntityTemplate template;
    /**
     * 扩展组件查找器.
     */
    @Mock
    private ExtensionComponentsFinder extensionComponentsFinder;
    /**
     * 附件驱动扫描器.
     */
    @Mock
    private AttachmentDriverFetcher fetcher;
    /**
     * 附件内容检查服务。
     */
    @Mock
    private AttachmentContentInspectionService contentInspectionService;
    /**
     * 被测试的附件驱动服务.
     */
    private AttachmentDriverServiceImpl service;
    /**
     * 驱动ID.
     */
    private UUID driverId;
    /**
     * 当前目录附件ID.
     */
    private UUID parentId;
    /**
     * 当前目录磁盘路径.
     */
    private String remotePath;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AttachmentDriverServiceImpl(driverRepository, attachmentRepository,
            eventPublisher, attachmentService, mountService, attachmentSha1Service, template,
            extensionComponentsFinder, contentInspectionService);
        driverId = UUID.randomUUID();
        parentId = UUID.randomUUID();
        remotePath = "D:/media";
        Attachment parentAttachment = Attachment
            .builder()
            .id(parentId)
            .driverId(driverId)
            .type(AttachmentType.Driver_Directory)
            .fsPath(remotePath)
            .build();
        AttachmentDriverEntity driver = AttachmentDriverEntity
            .builder()
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
        when(contentInspectionService.inspect(any(Attachment.class), eq(fetcher)))
            .thenReturn(Mono.just(new MediaFileDetectionResult(MediaFileFormat.MATROSKA)));
        when(attachmentService.save(any(Attachment.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(attachmentService.removeByIdOnlyRecords(any(UUID.class))).thenReturn(Mono.empty());
    }

    @Test
    void refreshSchedulesSha1ForEveryFileOnFirstScan() {
        Attachment firstFile = scannedFile("D:/media/first.mkv", 100L,
            LocalDateTime.of(2026, 7, 30, 12, 0));
        Attachment secondFile = scannedFile("D:/media/second.mkv", 200L,
            LocalDateTime.of(2026, 7, 30, 12, 1));
        when(fetcher.getChildren(driverId, parentId, remotePath))
            .thenReturn(Flux.just(firstFile, secondFile));
        when(attachmentRepository.findAllByParentIdAndDriverId(parentId, driverId))
            .thenReturn(Flux.empty());
        StepVerifier
            .create(service.refresh(parentId))
            .verifyComplete();

        verify(attachmentService, times(2)).save(any(Attachment.class));
        verify(attachmentSha1Service).calculateAsync(
            eq(fetcher), argThat(attachments ->
                attachments.equals(List.of(firstFile, secondFile))));
    }

    @Test
    void refreshMountRootKeepsDirectoriesAndRemovesFiles() {
        Attachment mountRoot = Attachment
            .builder()
            .id(parentId)
            .parentId(AttachmentConst.ROOT_DIRECTORY_ID)
            .driverId(driverId)
            .type(AttachmentType.Driver_Directory)
            .fsPath(remotePath)
            .build();
        Attachment rootFile = scannedFile("D:/media/root.mkv", 100L,
            LocalDateTime.of(2026, 8, 11, 12, 0));
        Attachment rootDirectory = Attachment
            .builder()
            .parentId(parentId)
            .driverId(driverId)
            .type(AttachmentType.Driver_Directory)
            .name("series")
            .fsPath("D:/media/series")
            .build();
        AttachmentEntity storedRootFile = storedFile(rootFile,
            rootFile.getModifiedTime(), "stored-sha1");
        when(attachmentService.findById(parentId)).thenReturn(Mono.just(mountRoot));
        when(fetcher.getChildren(driverId, parentId, remotePath))
            .thenReturn(Flux.just(rootFile, rootDirectory));
        when(attachmentRepository.findAllByParentIdAndDriverId(parentId, driverId))
            .thenReturn(Flux.just(storedRootFile));

        StepVerifier.create(service.refresh(parentId)).verifyComplete();

        verify(attachmentService).save(rootDirectory);
        verify(attachmentService, never()).save(rootFile);
        verify(contentInspectionService, never()).inspect(rootFile, fetcher);
        verify(attachmentService).removeByIdOnlyRecords(storedRootFile.getId());
        verify(attachmentSha1Service).calculateAsync(fetcher, List.of());
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

        StepVerifier
            .create(service.refresh(parentId))
            .verifyComplete();

        verify(fetcher, never()).calculateSha1(any(Attachment.class));
        verify(attachmentService, never()).save(any(Attachment.class));
        verify(attachmentService, never()).removeByIdOnlyRecords(any(UUID.class));
        verify(attachmentSha1Service).calculateAsync(fetcher, List.of());
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
        StepVerifier
            .create(service.refresh(parentId))
            .verifyComplete();

        ArgumentCaptor<Attachment> attachmentCaptor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentService).save(attachmentCaptor.capture());
        assertThat(attachmentCaptor
            .getValue()
            .getId()).isEqualTo(storedChangedFile.getId());
        assertThat(attachmentCaptor
            .getValue()
            .getSha1()).isNull();
        verify(attachmentService).removeByIdOnlyRecords(missingFile.getId());
        verify(attachmentSha1Service).calculateAsync(fetcher, List.of(changedFile));
    }

    @Test
    void refreshSkipsUnsupportedDriverNameBeforeInspectionAndSave() {
        Attachment unknownFile = scannedFile("D:/media/payload.exe", 101L,
            LocalDateTime.of(2026, 7, 30, 12, 0));
        when(fetcher.getChildren(driverId, parentId, remotePath))
            .thenReturn(Flux.just(unknownFile));
        when(attachmentRepository.findAllByParentIdAndDriverId(parentId, driverId))
            .thenReturn(Flux.empty());

        StepVerifier
            .create(service.refresh(parentId))
            .verifyComplete();

        verify(contentInspectionService, never()).inspect(any(Attachment.class), eq(fetcher));
        verify(attachmentService, never()).save(any(Attachment.class));
        verify(attachmentSha1Service).calculateAsync(fetcher, List.of());
    }

    @Test
    void refreshRemovesStoredFileWhenContentInspectionFails() {
        Attachment invalidFile = scannedFile("D:/media/payload.mp4", 101L,
            LocalDateTime.of(2026, 7, 30, 12, 0));
        AttachmentEntity storedInvalidFile = storedFile(invalidFile,
            invalidFile.getModifiedTime(), "old-sha1");
        when(fetcher.getChildren(driverId, parentId, remotePath))
            .thenReturn(Flux.just(invalidFile));
        when(attachmentRepository.findAllByParentIdAndDriverId(parentId, driverId))
            .thenReturn(Flux.just(storedInvalidFile));
        when(contentInspectionService.inspect(invalidFile, fetcher))
            .thenReturn(Mono.error(new IllegalArgumentException("invalid media")));

        StepVerifier
            .create(service.refresh(parentId))
            .verifyComplete();

        verify(attachmentService, never()).save(any(Attachment.class));
        verify(attachmentService).removeByIdOnlyRecords(storedInvalidFile.getId());
        verify(attachmentSha1Service).calculateAsync(fetcher, List.of());
    }

    @Test
    void refreshMergesConcurrentRequestsForSameDirectory() {
        Attachment file = scannedFile("D:/media/concurrent.mkv", 100L,
            LocalDateTime.of(2026, 7, 30, 12, 0));
        when(fetcher.getChildren(driverId, parentId, remotePath))
            .thenReturn(Flux
                .just(file)
                .delayElements(Duration.ofMillis(50)));
        when(attachmentRepository.findAllByParentIdAndDriverId(parentId, driverId))
            .thenReturn(Flux.empty());
        StepVerifier
            .create(Flux.merge(service.refresh(parentId), service.refresh(parentId)))
            .verifyComplete();

        verify(fetcher).getChildren(driverId, parentId, remotePath);
        verify(attachmentService).save(file);
        verify(attachmentSha1Service).calculateAsync(fetcher, List.of(file));
    }

    @Test
    void saveRebindsEnabledDriverWhenRemotePathChanges() {
        String newRemotePath = "D:/new-media";
        Sinks.One<Void> rebindCompletion = Sinks.one();
        AttachmentDriverEntity storedDriver = AttachmentDriverEntity
            .builder()
            .id(driverId)
            .enable(true)
            .type(AttachmentDriverType.LOCAL)
            .name(LocalDiskAttachmentDriverFetcher.LOCAL_DISK_DRIVER_NAME)
            .mountName("media")
            .remotePath(remotePath)
            .build();
        AttachmentDriver changedDriver = AttachmentDriver
            .builder()
            .id(driverId)
            .enable(false)
            .type(AttachmentDriverType.LOCAL)
            .name(LocalDiskAttachmentDriverFetcher.LOCAL_DISK_DRIVER_NAME)
            .mountName("media")
            .remotePath(newRemotePath)
            .build();
        when(driverRepository.findById(driverId)).thenReturn(Mono.just(storedDriver));
        when(driverRepository.update(any(AttachmentDriverEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(mountService.rebind(any(AttachmentDriverEntity.class),
            any(AttachmentDriverEntity.class))).thenReturn(rebindCompletion.asMono());

        StepVerifier
            .create(service.save(changedDriver))
            .expectSubscription()
            .expectNoEvent(Duration.ofMillis(10))
            .then(rebindCompletion::tryEmitEmpty)
            .assertNext(savedDriver -> {
                assertThat(savedDriver.getRemotePath()).isEqualTo(newRemotePath);
                assertThat(savedDriver.isEnable()).isTrue();
            })
            .verifyComplete();

        verify(mountService).rebind(
            argThat(driver -> remotePath.equals(driver.getRemotePath())),
            argThat(driver -> newRemotePath.equals(driver.getRemotePath())));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void saveCreatesDriverDisabledWhenRequestContainsEnableTrue() {
        AttachmentDriver newDriver = AttachmentDriver
            .builder()
            .enable(true)
            .type(AttachmentDriverType.LOCAL)
            .name(LocalDiskAttachmentDriverFetcher.LOCAL_DISK_DRIVER_NAME)
            .mountName("media")
            .remotePath(remotePath)
            .build();
        when(driverRepository.findByTypeAndNameAndMountName(
            AttachmentDriverType.LOCAL.name(),
            LocalDiskAttachmentDriverFetcher.LOCAL_DISK_DRIVER_NAME,
            "media")).thenReturn(Mono.empty());
        when(driverRepository.insert(any(AttachmentDriverEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier
            .create(service.save(newDriver))
            .assertNext(savedDriver -> {
                assertThat(savedDriver.getId()).isNotNull();
                assertThat(savedDriver.isEnable()).isFalse();
            })
            .verifyComplete();

        verify(eventPublisher, never()).publishEvent(
            any(AttachmentDriverEnableEvent.class));
    }

    @Test
    void saveNormalizesLocalDriverImplementationAndTokens() {
        AttachmentDriver newDriver = AttachmentDriver
            .builder()
            .type(AttachmentDriverType.LOCAL)
            .name("IGNORED")
            .mountName("media")
            .remotePath(remotePath)
            .accessToken("ignored-access")
            .refreshToken("ignored-refresh")
            .build();
        when(driverRepository.findByTypeAndNameAndMountName(
            AttachmentDriverType.LOCAL.name(),
            LocalDiskAttachmentDriverFetcher.LOCAL_DISK_DRIVER_NAME,
            "media")).thenReturn(Mono.empty());
        when(driverRepository.insert(any(AttachmentDriverEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier
            .create(service.save(newDriver))
            .assertNext(savedDriver -> {
                assertThat(savedDriver.getName())
                    .isEqualTo(LocalDiskAttachmentDriverFetcher.LOCAL_DISK_DRIVER_NAME);
                assertThat(savedDriver.getAccessToken()).isNull();
                assertThat(savedDriver.getRefreshToken()).isNull();
            })
            .verifyComplete();
    }

    @Test
    void saveRejectsCustomDriverWithoutMatchingPluginFetcher() {
        AttachmentDriver customDriver = AttachmentDriver
            .builder()
            .type(AttachmentDriverType.CUSTOM)
            .name("MISSING")
            .mountName("cloud")
            .remotePath("root")
            .build();

        assertThatThrownBy(() -> service.save(customDriver))
            .isInstanceOf(NoAvailableAttDriverFetcherException.class)
            .hasMessageContaining("CUSTOM")
            .hasMessageContaining("MISSING");

        verify(driverRepository, never()).insert(any(AttachmentDriverEntity.class));
    }

    @Test
    void listAttachmentsRefreshesDirectoryBeforeReturningFiles() {
        Attachment video = scannedFile("D:/media/episode.mkv", 100L,
            LocalDateTime.of(2026, 7, 30, 12, 0));
        AttachmentSearchCondition condition = AttachmentSearchCondition
            .builder()
            .parentId(parentId)
            .refresh(true)
            .build();
        PagingWrap<Attachment> page = new PagingWrap<>(1, 10, 1L, List.of(video));
        when(fetcher.getChildren(driverId, parentId, remotePath)).thenReturn(Flux.just(video));
        when(attachmentRepository.findAllByParentIdAndDriverId(parentId, driverId))
            .thenReturn(Flux.empty());
        when(attachmentService.listByCondition(condition)).thenReturn(Mono.just(page));

        StepVerifier
            .create(service.listAttachmentsByCondition(condition))
            .assertNext(result -> assertThat(result.getItems()).containsExactly(video))
            .verifyComplete();

        verify(attachmentService).save(video);
        verify(attachmentSha1Service).calculateAsync(fetcher, List.of(video));
        verify(attachmentService).listByCondition(condition);
    }

    @Test
    void refreshRejectsMissingAttachment() {
        UUID missingAttachmentId = UUID.randomUUID();
        when(attachmentService.findById(missingAttachmentId)).thenReturn(Mono.empty());

        StepVerifier
            .create(service.refresh(missingAttachmentId))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(AttachmentNotFoundException.class);
                assertThat(error).hasMessageContaining(missingAttachmentId.toString());
            })
            .verify();
    }

    @Test
    void refreshRejectsNonDriverDirectory() {
        UUID fileId = UUID.randomUUID();
        Attachment file = Attachment
            .builder()
            .id(fileId)
            .driverId(driverId)
            .type(AttachmentType.Driver_File)
            .build();
        when(attachmentService.findById(fileId)).thenReturn(Mono.just(file));

        StepVerifier
            .create(service.refresh(fileId))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(IllegalArgumentException.class);
                assertThat(error).hasMessageContaining(fileId.toString());
            })
            .verify();
    }

    @Test
    void refreshRejectsDirectoryWithoutDriverId() {
        UUID directoryId = UUID.randomUUID();
        Attachment directory = Attachment
            .builder()
            .id(directoryId)
            .type(AttachmentType.Driver_Directory)
            .build();
        when(attachmentService.findById(directoryId)).thenReturn(Mono.just(directory));

        StepVerifier
            .create(service.refresh(directoryId))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(IllegalStateException.class);
                assertThat(error).hasMessageContaining(directoryId.toString());
            })
            .verify();
    }

    @Test
    void refreshRejectsMissingDriver() {
        when(driverRepository.findById(driverId)).thenReturn(Mono.empty());

        StepVerifier
            .create(service.refresh(parentId))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(IllegalStateException.class);
                assertThat(error).hasMessageContaining(driverId.toString());
            })
            .verify();
    }

    private Attachment scannedFile(String fsPath, Long size, LocalDateTime modifiedTime) {
        return Attachment
            .builder()
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
        return AttachmentEntity
            .builder()
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
