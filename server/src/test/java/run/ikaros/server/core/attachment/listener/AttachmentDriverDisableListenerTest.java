package run.ikaros.server.core.attachment.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.server.core.attachment.event.AttachmentDriverDisableEvent;
import run.ikaros.server.core.attachment.service.AttachmentDriverMountService;
import run.ikaros.server.store.entity.AttachmentDriverEntity;

/** 本地附件驱动禁用监听器测试. */
class AttachmentDriverDisableListenerTest {
    @Test
    void disableUnmountsDriver() {
        AttachmentDriverEntity driver = AttachmentDriverEntity.builder()
            .id(UUID.randomUUID())
            .type(AttachmentDriverType.LOCAL)
            .name("DISK")
            .mountName("favorites")
            .remotePath("D:/media")
            .build();
        AttachmentDriverMountService mountService =
            Mockito.mock(AttachmentDriverMountService.class);
        when(mountService.unmount(driver)).thenReturn(Mono.empty());
        AttachmentDriverDisableListener listener =
            new AttachmentDriverDisableListener(mountService);

        StepVerifier.create(listener.onAttachmentDriverEnableEvent(
                new AttachmentDriverDisableEvent(this, driver)))
            .verifyComplete();

        verify(mountService).unmount(driver);
    }
}
