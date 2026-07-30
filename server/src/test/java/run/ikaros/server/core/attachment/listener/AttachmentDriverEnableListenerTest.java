package run.ikaros.server.core.attachment.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.server.core.attachment.event.AttachmentDriverEnableEvent;
import run.ikaros.server.core.attachment.service.AttachmentDriverMountService;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
import run.ikaros.server.store.repository.AttachmentDriverRepository;

/**
 * 本地附件驱动启用监听器测试.
 */
class AttachmentDriverEnableListenerTest {
    /** 附件驱动挂载服务. */
    @Mock
    private AttachmentDriverMountService mountService;
    /** 附件驱动仓库. */
    @Mock
    private AttachmentDriverRepository driverRepository;
    /** 被测试的监听器. */
    private AttachmentDriverEnableListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new AttachmentDriverEnableListener(mountService, driverRepository);
    }

    @Test
    void enableMountsDriver() {
        AttachmentDriverEntity driver = createDriver();
        when(mountService.mount(driver)).thenReturn(Mono.empty());

        StepVerifier.create(listener.onAttachmentDriverEnableEvent(
                new AttachmentDriverEnableEvent(this, driver)))
            .verifyComplete();

        verify(mountService).mount(driver);
    }

    @Test
    void initializeMountsAllEnabledLocalDrivers() {
        AttachmentDriverEntity driver = createDriver();
        when(driverRepository.findAllByTypeAndEnable(AttachmentDriverType.LOCAL.name(), true))
            .thenReturn(Flux.just(driver));
        when(mountService.mount(driver)).thenReturn(Mono.empty());

        StepVerifier.create(listener.initialize()).verifyComplete();

        verify(mountService).mount(driver);
    }

    private AttachmentDriverEntity createDriver() {
        return AttachmentDriverEntity.builder()
            .id(UUID.randomUUID())
            .enable(true)
            .type(AttachmentDriverType.LOCAL)
            .name("DISK")
            .mountName("favorites")
            .remotePath("D:/media")
            .build();
    }
}
