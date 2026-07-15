package run.ikaros.server.core.attachment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.core.attachment.exception.NoAvailableAttDriverFetcherException;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.attachment.event.AttachmentDriverDisableEvent;
import run.ikaros.server.core.attachment.event.AttachmentDriverEnableEvent;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.core.attachment.vo.AttachmentDriverFetcherVo;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
import run.ikaros.server.store.repository.AttachmentDriverRepository;
import run.ikaros.server.store.repository.AttachmentRepository;

class AttachmentDriverServiceImplTest {
    private AttachmentDriverRepository repository;
    private AttachmentRepository attachmentRepository;
    private ApplicationEventPublisher eventPublisher;
    private AttachmentService attachmentService;
    private R2dbcEntityTemplate template;
    private ExtensionComponentsFinder extensionComponentsFinder;
    private AttachmentDriverServiceImpl attachmentDriverService;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AttachmentDriverRepository.class);
        attachmentRepository = Mockito.mock(AttachmentRepository.class);
        eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        attachmentService = Mockito.mock(AttachmentService.class);
        template = Mockito.mock(R2dbcEntityTemplate.class);
        extensionComponentsFinder = Mockito.mock(ExtensionComponentsFinder.class);
        attachmentDriverService = new AttachmentDriverServiceImpl(
            repository, attachmentRepository, eventPublisher, attachmentService,
            template, extensionComponentsFinder
        );
    }

    // === save() tests ===

    @Test
    void save_whenDriverExists_updates() {
        UUID driverId = UuidV7Utils.generateUuid();
        AttachmentDriver driver = new AttachmentDriver();
        driver.setType(AttachmentDriverType.LOCAL);
        driver.setName("local-fs");
        driver.setMountName("default");

        AttachmentDriverEntity existingEntity = new AttachmentDriverEntity();
        existingEntity.setId(driverId);
        existingEntity.setType(AttachmentDriverType.LOCAL);
        existingEntity.setName("local-fs");
        existingEntity.setMountName("default");

        AttachmentDriverFetcher fetcher = Mockito.mock(AttachmentDriverFetcher.class);
        when(fetcher.getDriverType()).thenReturn(AttachmentDriverType.LOCAL);
        when(fetcher.getDriverName()).thenReturn("local-fs");
        when(extensionComponentsFinder.getExtensions(AttachmentDriverFetcher.class))
            .thenReturn(List.of(fetcher));
        when(repository.findByTypeAndNameAndMountName(
            eq("LOCAL"), eq("local-fs"), eq("default")))
            .thenReturn(Mono.just(existingEntity));
        when(repository.update(any(AttachmentDriverEntity.class)))
            .thenReturn(Mono.just(existingEntity));

        StepVerifier.create(attachmentDriverService.save(driver))
            .assertNext(result -> assertThat(result).isNotNull())
            .verifyComplete();
    }

    @Test
    void save_whenNoFetcher_throwsException() {
        AttachmentDriver driver = new AttachmentDriver();
        driver.setType(AttachmentDriverType.LOCAL);
        driver.setName("unknown-driver");

        when(extensionComponentsFinder.getExtensions(AttachmentDriverFetcher.class))
            .thenReturn(List.of());

        assertThrows(NoAvailableAttDriverFetcherException.class,
            () -> attachmentDriverService.save(driver));
    }

    @Test
    void save_withNullDriver_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> attachmentDriverService.save(null));
    }

    // === removeById() tests ===

    @Test
    void removeById() {
        UUID driverId = UuidV7Utils.generateUuid();
        AttachmentDriverEntity entity = new AttachmentDriverEntity();
        entity.setId(driverId);

        when(repository.findById(driverId)).thenReturn(Mono.just(entity));
        when(repository.deleteById(driverId)).thenReturn(Mono.empty());

        StepVerifier.create(attachmentDriverService.removeById(driverId))
            .verifyComplete();
        verify(eventPublisher).publishEvent(any(AttachmentDriverDisableEvent.class));
    }

    @Test
    void removeById_withNullId_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> attachmentDriverService.removeById(null));
    }

    @Test
    void removeById_whenNotFound() {
        UUID driverId = UuidV7Utils.generateUuid();
        when(repository.findById(driverId)).thenReturn(Mono.empty());

        StepVerifier.create(attachmentDriverService.removeById(driverId))
            .verifyComplete();
    }

    // === removeByTypeAndName() tests ===

    @Test
    void removeByTypeAndName() {
        AttachmentDriverEntity entity = new AttachmentDriverEntity();
        entity.setId(UuidV7Utils.generateUuid());

        when(repository.findByTypeAndName("LOCAL", "local-fs"))
            .thenReturn(Mono.just(entity));
        when(repository.deleteById(entity.getId())).thenReturn(Mono.empty());

        StepVerifier.create(
                attachmentDriverService.removeByTypeAndName("LOCAL", "local-fs"))
            .verifyComplete();
        verify(eventPublisher).publishEvent(any(AttachmentDriverDisableEvent.class));
    }

    @Test
    void removeByTypeAndName_withNullType_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> attachmentDriverService.removeByTypeAndName(null, "local-fs"));
    }

    // === findById() tests ===

    @Test
    void findById() {
        UUID driverId = UuidV7Utils.generateUuid();
        AttachmentDriverEntity entity = new AttachmentDriverEntity();
        entity.setId(driverId);
        entity.setType(AttachmentDriverType.LOCAL);
        entity.setName("local-fs");

        when(repository.findById(driverId)).thenReturn(Mono.just(entity));

        StepVerifier.create(attachmentDriverService.findById(driverId))
            .assertNext(driver -> {
                assertThat(driver.getId()).isEqualTo(driverId);
                assertThat(driver.getType()).isEqualTo(AttachmentDriverType.LOCAL);
                assertThat(driver.getName()).isEqualTo("local-fs");
            })
            .verifyComplete();
    }

    @Test
    void findById_withNullId_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> attachmentDriverService.findById(null));
    }

    @Test
    void findById_whenNotFound() {
        UUID driverId = UuidV7Utils.generateUuid();
        when(repository.findById(driverId)).thenReturn(Mono.empty());
        StepVerifier.create(attachmentDriverService.findById(driverId))
            .verifyComplete();
    }

    // === findByTypeAndName() tests ===

    @Test
    void findByTypeAndName() {
        AttachmentDriverEntity entity = new AttachmentDriverEntity();
        entity.setId(UuidV7Utils.generateUuid());
        entity.setType(AttachmentDriverType.LOCAL);
        entity.setName("local-fs");

        when(repository.findByTypeAndName("LOCAL", "local-fs"))
            .thenReturn(Mono.just(entity));

        StepVerifier.create(attachmentDriverService.findByTypeAndName("LOCAL", "local-fs"))
            .assertNext(driver -> assertThat(driver.getName()).isEqualTo("local-fs"))
            .verifyComplete();
    }

    @Test
    void findByTypeAndName_withNullType_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> attachmentDriverService.findByTypeAndName(null, "local-fs"));
    }

    // === enable/disable tests ===

    @Test
    void enable() {
        UUID driverId = UuidV7Utils.generateUuid();
        AttachmentDriverEntity entity = new AttachmentDriverEntity();
        entity.setId(driverId);
        entity.setEnable(false);

        when(repository.findById(driverId)).thenReturn(Mono.just(entity));
        when(repository.update(any(AttachmentDriverEntity.class)))
            .thenReturn(Mono.just(entity.setEnable(true)));

        StepVerifier.create(attachmentDriverService.enable(driverId))
            .verifyComplete();
        verify(eventPublisher).publishEvent(any(AttachmentDriverEnableEvent.class));
    }

    @Test
    void disable() {
        UUID driverId = UuidV7Utils.generateUuid();
        AttachmentDriverEntity entity = new AttachmentDriverEntity();
        entity.setId(driverId);
        entity.setEnable(true);

        when(repository.findById(driverId)).thenReturn(Mono.just(entity));
        when(repository.update(any(AttachmentDriverEntity.class)))
            .thenReturn(Mono.just(entity.setEnable(false)));

        StepVerifier.create(attachmentDriverService.disable(driverId))
            .verifyComplete();
        verify(eventPublisher).publishEvent(any(AttachmentDriverDisableEvent.class));
    }

    @Test
    void enable_withNullId_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> attachmentDriverService.enable(null));
    }

    // === listAttachmentsByCondition tests ===

    @Test
    void listAttachmentsByCondition_withoutRefresh() {
        AttachmentSearchCondition condition = AttachmentSearchCondition.builder()
            .page(1).size(10).refresh(false)
            .parentId(UuidV7Utils.generateUuid()).build();

        when(attachmentService.listByCondition(condition))
            .thenReturn(Mono.just(new PagingWrap<>(1, 10, 0L, List.of())));

        StepVerifier.create(attachmentDriverService.listAttachmentsByCondition(condition))
            .assertNext(wrap -> assertThat(wrap.getTotal()).isZero())
            .verifyComplete();
    }

    @Test
    void listAttachmentsByCondition_withRefresh() {
        UUID parentId = UuidV7Utils.generateUuid();
        AttachmentSearchCondition condition = AttachmentSearchCondition.builder()
            .page(1).size(10).refresh(true).parentId(parentId).build();

        Attachment attachment = new Attachment();
        attachment.setId(parentId);
        attachment.setType(AttachmentType.Driver_Directory);
        attachment.setDriverId(UuidV7Utils.generateUuid());

        AttachmentDriverEntity driverEntity = new AttachmentDriverEntity();
        driverEntity.setId(attachment.getDriverId());
        driverEntity.setType(AttachmentDriverType.LOCAL);
        driverEntity.setName("local-fs");

        AttachmentDriverFetcher fetcher = Mockito.mock(AttachmentDriverFetcher.class);
        when(fetcher.getDriverType()).thenReturn(AttachmentDriverType.LOCAL);
        when(fetcher.getDriverName()).thenReturn("local-fs");

        when(attachmentService.findById(parentId)).thenReturn(Mono.just(attachment));
        when(repository.findById(attachment.getDriverId())).thenReturn(Mono.just(driverEntity));
        when(extensionComponentsFinder.getExtensions(AttachmentDriverFetcher.class))
            .thenReturn(List.of(fetcher));
        when(fetcher.getChildren(any(), any(), any())).thenReturn(Flux.empty());
        when(attachmentService.listByCondition(condition))
            .thenReturn(Mono.just(new PagingWrap<>(1, 10, 0L, List.of())));

        StepVerifier.create(attachmentDriverService.listAttachmentsByCondition(condition))
            .assertNext(wrap -> assertThat(wrap.getTotal()).isZero())
            .verifyComplete();
    }

    @Test
    void listAttachmentsByCondition_withNullRefresh_throwsException() {
        AttachmentSearchCondition condition = AttachmentSearchCondition.builder()
            .page(1).size(10).build();

        assertThrows(IllegalArgumentException.class,
            () -> attachmentDriverService.listAttachmentsByCondition(condition));
    }

    // === listDriversByCondition tests ===

    @Test
    void listDriversByCondition_withDefaults() {
        AttachmentDriverEntity entity = new AttachmentDriverEntity();
        entity.setId(UuidV7Utils.generateUuid());
        entity.setType(AttachmentDriverType.LOCAL);
        entity.setName("local-fs");

        when(template.select(any(Query.class), eq(AttachmentDriverEntity.class)))
            .thenReturn(Flux.just(entity));
        when(template.count(any(Query.class), eq(AttachmentDriverEntity.class)))
            .thenReturn(Mono.just(1L));

        StepVerifier.create(attachmentDriverService.listDriversByCondition(null, null))
            .assertNext(wrap -> {
                assertThat(wrap.getPage()).isEqualTo(1);
                assertThat(wrap.getSize()).isEqualTo(10);
                assertThat(wrap.getTotal()).isEqualTo(1);
            })
            .verifyComplete();
    }

    @Test
    void listDriversByCondition_withCustomPage() {
        AttachmentDriverEntity entity = new AttachmentDriverEntity();
        entity.setId(UuidV7Utils.generateUuid());
        entity.setType(AttachmentDriverType.LOCAL);
        entity.setName("local-fs");

        when(template.select(any(Query.class), eq(AttachmentDriverEntity.class)))
            .thenReturn(Flux.just(entity));
        when(template.count(any(Query.class), eq(AttachmentDriverEntity.class)))
            .thenReturn(Mono.just(1L));

        StepVerifier.create(attachmentDriverService.listDriversByCondition(2, 20))
            .assertNext(wrap -> {
                assertThat(wrap.getPage()).isEqualTo(2);
                assertThat(wrap.getSize()).isEqualTo(20);
            })
            .verifyComplete();
    }

    // === listDriversFetchers test ===

    @Test
    void listDriversFetchers() {
        AttachmentDriverFetcher fetcher1 = Mockito.mock(AttachmentDriverFetcher.class);
        when(fetcher1.getDriverType()).thenReturn(AttachmentDriverType.LOCAL);
        when(fetcher1.getDriverName()).thenReturn("local-fs");

        AttachmentDriverFetcher fetcher2 = Mockito.mock(AttachmentDriverFetcher.class);
        when(fetcher2.getDriverType()).thenReturn(AttachmentDriverType.WEBDAV);
        when(fetcher2.getDriverName()).thenReturn("webdav-fs");

        when(extensionComponentsFinder.getExtensions(AttachmentDriverFetcher.class))
            .thenReturn(List.of(fetcher1, fetcher2));

        StepVerifier.create(attachmentDriverService.listDriversFetchers())
            .expectNextMatches(vo -> "local-fs".equals(vo.getName())
                && AttachmentDriverType.LOCAL == vo.getType())
            .expectNextMatches(vo -> "webdav-fs".equals(vo.getName())
                && AttachmentDriverType.WEBDAV == vo.getType())
            .verifyComplete();
    }
}
