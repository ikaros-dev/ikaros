package run.ikaros.server.core.attachment.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static run.ikaros.api.core.attachment.AttachmentConst.DRIVER_STATIC_RESOURCE_PREFIX;
import static run.ikaros.api.core.attachment.AttachmentConst.ROOT_DIRECTORY_ID;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.config.DynamicDirectoryResolver;
import run.ikaros.server.core.attachment.event.AttachmentDriverEnableEvent;
import run.ikaros.server.core.attachment.extension.LocalAttachmentPathValidator;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
import run.ikaros.server.store.repository.AttachmentDriverRepository;

/**
 * 本地附件驱动启用监听器测试.
 */
class AttachmentDriverEnableListenerTest {
    /** 附件服务. */
    @Mock
    private AttachmentService attachmentService;
    /** 附件驱动仓库. */
    @Mock
    private AttachmentDriverRepository driverRepository;
    /** 动态目录解析器. */
    private DynamicDirectoryResolver directoryResolver;
    /** 本地驱动路径校验器. */
    private LocalAttachmentPathValidator pathValidator;
    /** 被测试的监听器. */
    private AttachmentDriverEnableListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        directoryResolver = new DynamicDirectoryResolver();
        pathValidator = new LocalAttachmentPathValidator();
        listener = new AttachmentDriverEnableListener(
            attachmentService, directoryResolver, driverRepository, pathValidator);
    }

    @Test
    void initializeMountsEnabledDriverAndCreatesMissingRoot(@TempDir Path tempDir)
        throws IOException {
        UUID driverId = UUID.randomUUID();
        AttachmentDriverEntity driver = AttachmentDriverEntity.builder()
            .id(driverId)
            .enable(true)
            .type(AttachmentDriverType.LOCAL)
            .name("DISK")
            .mountName("favorites")
            .remotePath(tempDir.toString())
            .build();
        when(driverRepository.findAllByTypeAndEnable("LOCAL", true))
            .thenReturn(Flux.just(driver));
        when(attachmentService.findByTypeAndParentIdAndName(
            AttachmentType.Driver_Directory, ROOT_DIRECTORY_ID, "favorites"))
            .thenReturn(Mono.empty());
        when(attachmentService.save(any(Attachment.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(listener.initialize()).verifyComplete();

        ArgumentCaptor<Attachment> attachmentCaptor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentService).save(attachmentCaptor.capture());
        Attachment rootAttachment = attachmentCaptor.getValue();
        assertThat(rootAttachment.getDriverId()).isEqualTo(driverId);
        assertThat(rootAttachment.getFsPath()).isEqualTo(tempDir.toString());
        assertThat(rootAttachment.getParentId()).isEqualTo(ROOT_DIRECTORY_ID);
        assertThat(directoryResolver.getAllMappings())
            .containsEntry("favorites", tempDir.toRealPath());
        StepVerifier.create(pathValidator.validate(driverId, tempDir.toString()))
            .expectNext(tempDir.toRealPath())
            .verifyComplete();
    }

    @Test
    void enableRebindsExistingMountAttachmentToCurrentDriver(@TempDir Path tempDir)
        throws IOException {
        UUID oldDriverId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        AttachmentDriverEntity driver = AttachmentDriverEntity.builder()
            .id(driverId)
            .enable(true)
            .type(AttachmentDriverType.LOCAL)
            .name("DISK")
            .mountName("favorites")
            .remotePath(tempDir.toString())
            .build();
        Attachment existingMount = Attachment.builder()
            .id(UUID.randomUUID())
            .parentId(UUID.randomUUID())
            .type(AttachmentType.Driver_Directory)
            .name("favorites")
            .url("/old-url")
            .driverId(oldDriverId)
            .fsPath("D:/old-media")
            .path("/old-path")
            .deleted(true)
            .build();
        when(attachmentService.findByTypeAndParentIdAndName(
            AttachmentType.Driver_Directory, ROOT_DIRECTORY_ID, "favorites"))
            .thenReturn(Mono.just(existingMount));
        when(attachmentService.save(any(Attachment.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(listener.onAttachmentDriverEnableEvent(
                new AttachmentDriverEnableEvent(this, driver)))
            .verifyComplete();

        ArgumentCaptor<Attachment> attachmentCaptor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentService).save(attachmentCaptor.capture());
        Attachment mountAttachment = attachmentCaptor.getValue();
        assertThat(mountAttachment.getId()).isEqualTo(existingMount.getId());
        assertThat(mountAttachment.getParentId()).isEqualTo(ROOT_DIRECTORY_ID);
        assertThat(mountAttachment.getType()).isEqualTo(AttachmentType.Driver_Directory);
        assertThat(mountAttachment.getName()).isEqualTo("favorites");
        assertThat(mountAttachment.getUrl())
            .isEqualTo(DRIVER_STATIC_RESOURCE_PREFIX + "/favorites");
        assertThat(mountAttachment.getDriverId()).isEqualTo(driverId);
        assertThat(mountAttachment.getFsPath()).isEqualTo(tempDir.toString());
        assertThat(mountAttachment.getPath()).isEqualTo("/favorites");
        assertThat(mountAttachment.getDeleted()).isFalse();
        assertThat(directoryResolver.getAllMappings())
            .containsEntry("favorites", tempDir.toRealPath());
        StepVerifier.create(pathValidator.validate(driverId, tempDir.toString()))
            .expectNext(tempDir.toRealPath())
            .verifyComplete();
    }

    @Test
    void enableRollsBackRegistrationsWhenMountSaveFails(@TempDir Path tempDir) {
        UUID driverId = UUID.randomUUID();
        AttachmentDriverEntity driver = AttachmentDriverEntity.builder()
            .id(driverId)
            .type(AttachmentDriverType.LOCAL)
            .name("DISK")
            .mountName("favorites")
            .remotePath(tempDir.toString())
            .build();
        when(attachmentService.findByTypeAndParentIdAndName(
            AttachmentType.Driver_Directory, ROOT_DIRECTORY_ID, "favorites"))
            .thenReturn(Mono.empty());
        when(attachmentService.save(any(Attachment.class)))
            .thenReturn(Mono.error(new IllegalStateException("save failed")));

        StepVerifier.create(listener.onAttachmentDriverEnableEvent(
                new AttachmentDriverEnableEvent(this, driver)))
            .expectErrorMessage("save failed")
            .verify();

        assertThat(directoryResolver.getAllMappings()).doesNotContainKey("favorites");
        StepVerifier.create(pathValidator.validate(driverId, tempDir.toString()))
            .expectError(IllegalStateException.class)
            .verify();
    }
}
