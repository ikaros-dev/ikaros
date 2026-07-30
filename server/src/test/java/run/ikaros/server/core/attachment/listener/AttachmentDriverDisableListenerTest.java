package run.ikaros.server.core.attachment.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static run.ikaros.api.core.attachment.AttachmentConst.ROOT_DIRECTORY_ID;

import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.config.DynamicDirectoryResolver;
import run.ikaros.server.core.attachment.event.AttachmentDriverDisableEvent;
import run.ikaros.server.core.attachment.extension.LocalAttachmentPathValidator;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentRepository;

/** 本地附件驱动禁用监听器测试. */
class AttachmentDriverDisableListenerTest {
    @Test
    void disableRemovesMountAndRevokesPathAccess(@TempDir Path tempDir) {
        UUID driverId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        final AttachmentDriverEntity driver = AttachmentDriverEntity.builder()
            .id(driverId)
            .type(AttachmentDriverType.LOCAL)
            .name("DISK")
            .mountName("favorites")
            .remotePath(tempDir.toString())
            .build();
        AttachmentRepository repository = Mockito.mock(AttachmentRepository.class);
        final AttachmentService attachmentService = Mockito.mock(AttachmentService.class);
        DynamicDirectoryResolver directoryResolver = new DynamicDirectoryResolver();
        LocalAttachmentPathValidator pathValidator = new LocalAttachmentPathValidator();
        directoryResolver.addDirectoryMapping("favorites", tempDir.toString());
        pathValidator.register(driverId, tempDir.toString());
        when(repository.findByTypeAndParentIdAndName(
            AttachmentType.Driver_Directory, ROOT_DIRECTORY_ID, "favorites"))
            .thenReturn(Mono.just(AttachmentEntity.builder().id(attachmentId).build()));
        when(attachmentService.removeByIdOnlyRecords(attachmentId)).thenReturn(Mono.empty());
        AttachmentDriverDisableListener listener = new AttachmentDriverDisableListener(
            repository, directoryResolver, attachmentService, pathValidator);

        StepVerifier.create(listener.onAttachmentDriverEnableEvent(
                new AttachmentDriverDisableEvent(this, driver)))
            .verifyComplete();

        assertThat(directoryResolver.getAllMappings()).doesNotContainKey("favorites");
        StepVerifier.create(pathValidator.validate(driverId, tempDir.toString()))
            .expectError(IllegalStateException.class)
            .verify();
        verify(attachmentService).removeByIdOnlyRecords(attachmentId);
    }
}
