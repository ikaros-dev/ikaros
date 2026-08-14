package run.ikaros.server.core.attachment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static run.ikaros.api.core.attachment.AttachmentConst.ROOT_DIRECTORY_ID;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.config.DynamicDirectoryResolver;
import run.ikaros.server.core.attachment.extension.LocalAttachmentPathValidator;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentRepository;

/**
 * 默认附件驱动挂载服务测试.
 */
@org.jspecify.annotations.NullUnmarked
class DefaultAttachmentDriverMountServiceTest {
    /** 附件服务. */
    @Mock
    private AttachmentService attachmentService;
    /** 附件仓库. */
    @Mock
    private AttachmentRepository attachmentRepository;
    /** 动态目录解析器. */
    private DynamicDirectoryResolver directoryResolver;
    /** 本地驱动路径校验器. */
    private LocalAttachmentPathValidator pathValidator;
    /** 被测试的挂载服务. */
    private DefaultAttachmentDriverMountService mountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        directoryResolver = new DynamicDirectoryResolver();
        pathValidator = new LocalAttachmentPathValidator();
        mountService = new DefaultAttachmentDriverMountService(
            attachmentService, attachmentRepository, directoryResolver, pathValidator);
    }

    @Test
    void mountRestoresExistingRootAttachment(@TempDir Path remotePath) throws IOException {
        UUID driverId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        AttachmentDriverEntity driver = createDriver(driverId, remotePath);
        Attachment existingAttachment = Attachment.builder()
            .id(attachmentId)
            .type(AttachmentType.Driver_Directory)
            .parentId(ROOT_DIRECTORY_ID)
            .name("media")
            .fsPath("D:/old-media")
            .deleted(Boolean.TRUE)
            .build();
        when(attachmentService.findByTypeAndParentIdAndName(
            AttachmentType.Driver_Directory, ROOT_DIRECTORY_ID, "media"))
            .thenReturn(Mono.just(existingAttachment));
        when(attachmentService.save(any(Attachment.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(mountService.mount(driver)).verifyComplete();

        ArgumentCaptor<Attachment> attachmentCaptor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentService).save(attachmentCaptor.capture());
        Attachment savedAttachment = attachmentCaptor.getValue();
        assertThat(savedAttachment.getId()).isEqualTo(attachmentId);
        assertThat(savedAttachment.getFsPath()).isEqualTo(remotePath.toString());
        assertThat(savedAttachment.getDeleted()).isFalse();
        assertThat(directoryResolver.getAllMappings())
            .containsEntry("media", remotePath.toRealPath());
        StepVerifier.create(pathValidator.validate(driverId, remotePath.toString()))
            .expectNext(remotePath.toRealPath())
            .verifyComplete();
    }

    @Test
    void unmountRemovesMappingAndRootAttachment(@TempDir Path remotePath) {
        UUID driverId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        final AttachmentDriverEntity driver = createDriver(driverId, remotePath);
        directoryResolver.addDirectoryMapping("media", remotePath.toString());
        pathValidator.register(driverId, remotePath.toString());
        when(attachmentRepository.findByTypeAndParentIdAndName(
            AttachmentType.Driver_Directory, ROOT_DIRECTORY_ID, "media"))
            .thenReturn(Mono.just(AttachmentEntity.builder().id(attachmentId).build()));
        when(attachmentService.removeByIdOnlyRecords(attachmentId)).thenReturn(Mono.empty());

        StepVerifier.create(mountService.unmount(driver)).verifyComplete();

        assertThat(directoryResolver.getAllMappings()).doesNotContainKey("media");
        StepVerifier.create(pathValidator.validate(driverId, remotePath.toString()))
            .expectError(IllegalStateException.class)
            .verify();
        verify(attachmentService).removeByIdOnlyRecords(attachmentId);
    }

    @Test
    void rebindSameMountUpdatesRootWithoutDeletingIt(@TempDir Path tempDir)
        throws IOException {
        Path previousPath = Files.createDirectory(tempDir.resolve("previous"));
        Path currentPath = Files.createDirectory(tempDir.resolve("current"));
        UUID driverId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        AttachmentDriverEntity previousDriver = createDriver(driverId, previousPath);
        AttachmentDriverEntity currentDriver = createDriver(driverId, currentPath);
        Attachment existingAttachment = Attachment.builder()
            .id(attachmentId)
            .type(AttachmentType.Driver_Directory)
            .parentId(ROOT_DIRECTORY_ID)
            .name("media")
            .fsPath(previousPath.toString())
            .deleted(Boolean.FALSE)
            .build();
        when(attachmentService.findByTypeAndParentIdAndName(
            AttachmentType.Driver_Directory, ROOT_DIRECTORY_ID, "media"))
            .thenReturn(Mono.just(existingAttachment));
        when(attachmentService.save(any(Attachment.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(mountService.rebind(previousDriver, currentDriver)).verifyComplete();

        ArgumentCaptor<Attachment> attachmentCaptor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentService).save(attachmentCaptor.capture());
        assertThat(attachmentCaptor.getValue().getId()).isEqualTo(attachmentId);
        assertThat(attachmentCaptor.getValue().getFsPath()).isEqualTo(currentPath.toString());
        assertThat(attachmentCaptor.getValue().getDeleted()).isFalse();
        verify(attachmentService, never()).removeByIdOnlyRecords(any(UUID.class));
        verify(attachmentRepository, never()).findByTypeAndParentIdAndName(
            AttachmentType.Driver_Directory, ROOT_DIRECTORY_ID, "media");
    }

    private AttachmentDriverEntity createDriver(UUID driverId, Path remotePath) {
        return AttachmentDriverEntity.builder()
            .id(driverId)
            .enable(true)
            .type(AttachmentDriverType.LOCAL)
            .name("DISK")
            .mountName("media")
            .remotePath(remotePath.toString())
            .build();
    }
}
