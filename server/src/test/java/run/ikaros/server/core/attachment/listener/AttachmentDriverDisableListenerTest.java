package run.ikaros.server.core.attachment.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.attachment.AttachmentConst;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.config.DynamicDirectoryResolver;
import run.ikaros.server.core.attachment.event.AttachmentDriverDisableEvent;
import run.ikaros.server.core.attachment.extension.LocalAttachmentPathValidator;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentRepository;

/** 附件驱动禁用监听器测试. */
class AttachmentDriverDisableListenerTest {
    @Test
    void disableRemovesMappingAndUnregistersValidator() {
        AttachmentDriverEntity driver = AttachmentDriverEntity.builder()
            .id(UUID.randomUUID())
            .type(AttachmentDriverType.LOCAL)
            .name("DISK")
            .mountName("favorites")
            .remotePath("D:/media")
            .build();

        AttachmentRepository attachmentRepository = mock(AttachmentRepository.class);
        DynamicDirectoryResolver dynamicDirectoryResolver = mock(DynamicDirectoryResolver.class);
        AttachmentService attachmentService = mock(AttachmentService.class);
        LocalAttachmentPathValidator pathValidator = mock(LocalAttachmentPathValidator.class);

        AttachmentEntity directory = AttachmentEntity.builder()
            .id(UUID.randomUUID())
            .build();
        when(attachmentRepository.findByTypeAndParentIdAndName(
            AttachmentType.Driver_Directory, AttachmentConst.ROOT_DIRECTORY_ID, "favorites"))
            .thenReturn(Mono.just(directory));
        when(attachmentService.removeByIdOnlyRecords(directory.getId()))
            .thenReturn(Mono.empty());

        AttachmentDriverDisableListener listener = new AttachmentDriverDisableListener(
            attachmentRepository, dynamicDirectoryResolver, attachmentService, pathValidator);

        StepVerifier.create(listener.onAttachmentDriverEnableEvent(
                new AttachmentDriverDisableEvent(this, driver)))
            .verifyComplete();

        verify(dynamicDirectoryResolver).removeDirectoryMapping("favorites");
        verify(pathValidator).unregister(driver.getId());
        verify(attachmentService).removeByIdOnlyRecords(directory.getId());
    }
}
