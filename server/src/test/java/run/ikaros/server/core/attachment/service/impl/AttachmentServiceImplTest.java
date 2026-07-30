package run.ikaros.server.core.attachment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentConst;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentDriverRepository;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.AttachmentRelationRepository;
import run.ikaros.server.store.repository.AttachmentRepository;

class AttachmentServiceImplTest {

    @Mock
    private AttachmentRepository repository;
    @Mock
    private AttachmentReferenceRepository referenceRepository;
    @Mock
    private AttachmentRelationRepository relationRepository;
    @Mock
    private R2dbcEntityTemplate template;
    @Mock
    private IkarosProperties ikarosProperties;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private AttachmentDriverRepository driverRepository;
    @Mock
    private ExtensionComponentsFinder extensionComponentsFinder;
    private AttachmentServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AttachmentServiceImpl(
            repository, referenceRepository, relationRepository,
            template, ikarosProperties, applicationEventPublisher,
            attachmentRepository, driverRepository, extensionComponentsFinder);
    }

    @Test
    void constructor_withNineParams() {
        assertThat(service).isNotNull();
    }

    @Test
    void save_allowsDriverDirectoryOutsideWorkDirectory(@TempDir Path tempDir) {
        Path workDirectory = tempDir.resolve("work");
        Path driverDirectory = tempDir.resolve("driver");
        Attachment attachment = Attachment.builder()
            .name("收藏")
            .type(AttachmentType.Driver_Directory)
            .parentId(AttachmentConst.ROOT_DIRECTORY_ID)
            .driverId(UUID.randomUUID())
            .fsPath(driverDirectory.toString())
            .build();
        when(ikarosProperties.getWorkDir()).thenReturn(workDirectory);
        when(repository.findByTypeAndParentIdAndName(
            AttachmentType.Driver_Directory,
            AttachmentConst.ROOT_DIRECTORY_ID,
            "收藏")).thenReturn(Mono.empty());
        when(attachmentRepository.insert(any(AttachmentEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.save(attachment))
            .assertNext(saved -> {
                assertThat(saved.getFsPath()).isEqualTo(driverDirectory.toString());
                assertThat(saved.getDriverId()).isEqualTo(attachment.getDriverId());
            })
            .verifyComplete();
    }

    @Test
    void save_allowsDriverFileOutsideWorkDirectory(@TempDir Path tempDir) {
        Path driverFile = tempDir.resolve("episode.mkv");
        Attachment attachment = Attachment.builder()
            .name("episode.mkv")
            .type(AttachmentType.Driver_File)
            .driverId(UUID.randomUUID())
            .fsPath(driverFile.toString())
            .build();
        when(repository.findByTypeAndParentIdAndName(
            AttachmentType.Driver_File,
            AttachmentConst.ROOT_DIRECTORY_ID,
            "episode.mkv")).thenReturn(Mono.empty());
        when(attachmentRepository.insert(any(AttachmentEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.save(attachment))
            .assertNext(saved -> {
                assertThat(saved.getFsPath()).isEqualTo(driverFile.toString());
                assertThat(saved.getDriverId()).isEqualTo(attachment.getDriverId());
            })
            .verifyComplete();
    }

    @Test
    void save_rejectsRegularAttachmentOutsideWorkDirectory(@TempDir Path tempDir) {
        Path workDirectory = tempDir.resolve("work");
        Path outsideFile = tempDir.resolve("outside.mkv");
        Attachment attachment = Attachment.builder()
            .name("outside.mkv")
            .type(AttachmentType.File)
            .fsPath(outsideFile.toString())
            .build();
        when(ikarosProperties.getWorkDir()).thenReturn(workDirectory);

        assertThatThrownBy(() -> service.save(attachment))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fsPath escapes work directory");
    }

    @Test
    void save_rejectsDriverAttachmentWithoutDriverId(@TempDir Path tempDir) {
        Attachment attachment = Attachment.builder()
            .name("driver")
            .type(AttachmentType.Driver_Directory)
            .fsPath(tempDir.toString())
            .build();

        assertThatThrownBy(() -> service.save(attachment))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("driverId");
    }

    // ===== findById =====

    @Test
    void findById_found() {
        UUID id = UuidV7Utils.generateUuid();
        AttachmentEntity entity = AttachmentEntity.builder()
            .id(id)
            .name("test-file.mp4")
            .type(AttachmentType.File)
            .parentId(AttachmentConst.ROOT_DIRECTORY_ID)
            .size(1024L)
            .build();

        when(repository.findById(id)).thenReturn(Mono.just(entity));

        StepVerifier.create(service.findById(id))
            .assertNext(attachment -> {
                assertThat(attachment.getId()).isEqualTo(id);
                assertThat(attachment.getName()).isEqualTo("test-file.mp4");
                assertThat(attachment.getType()).isEqualTo(AttachmentType.File);
                assertThat(attachment.getSize()).isEqualTo(1024L);
            })
            .verifyComplete();
    }

    @Test
    void findById_notFound() {
        UUID id = UuidV7Utils.generateUuid();
        when(repository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(service.findById(id))
            .verifyComplete();
    }

    // ===== findByTypeAndParentIdAndName =====

    @Test
    void findByTypeAndParentIdAndName_found() {
        UUID parentId = UuidV7Utils.generateUuid();
        String name = "test-video.mkv";

        AttachmentEntity entity = AttachmentEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .name(name)
            .type(AttachmentType.File)
            .parentId(parentId)
            .size(2048L)
            .build();

        when(repository.findByTypeAndParentIdAndName(AttachmentType.File, parentId, name))
            .thenReturn(Mono.just(entity));

        StepVerifier.create(service.findByTypeAndParentIdAndName(
                AttachmentType.File, parentId, name))
            .assertNext(attachment -> {
                assertThat(attachment.getName()).isEqualTo(name);
                assertThat(attachment.getType()).isEqualTo(AttachmentType.File);
                assertThat(attachment.getParentId()).isEqualTo(parentId);
            })
            .verifyComplete();
    }

    @Test
    void findByTypeAndParentIdAndName_notFound() {
        UUID parentId = UuidV7Utils.generateUuid();
        String name = "nonexistent.txt";

        when(repository.findByTypeAndParentIdAndName(
            AttachmentType.File, parentId, name))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.findByTypeAndParentIdAndName(
                AttachmentType.File, parentId, name))
            .verifyComplete();
    }

    @Test
    void findByTypeAndParentIdAndName_nullParentId_defaultsToRoot() {
        String name = "test.txt";

        AttachmentEntity entity = AttachmentEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .name(name)
            .type(AttachmentType.File)
            .parentId(AttachmentConst.ROOT_DIRECTORY_ID)
            .size(100L)
            .build();

        when(repository.findByTypeAndParentIdAndName(
            AttachmentType.File, AttachmentConst.ROOT_DIRECTORY_ID, name))
            .thenReturn(Mono.just(entity));

        StepVerifier.create(service.findByTypeAndParentIdAndName(
                AttachmentType.File, null, name))
            .assertNext(attachment -> {
                assertThat(attachment.getName()).isEqualTo(name);
                assertThat(attachment.getParentId()).isEqualTo(AttachmentConst.ROOT_DIRECTORY_ID);
            })
            .verifyComplete();
    }

    @Test
    void findByTypeAndParentIdAndName_nullType_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.findByTypeAndParentIdAndName(
                null, UuidV7Utils.generateUuid(), "test.txt"));
    }

    @Test
    void findByTypeAndParentIdAndName_emptyName_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.findByTypeAndParentIdAndName(
                AttachmentType.File, UuidV7Utils.generateUuid(), ""));
    }

    // ===== existsByParentIdAndName =====

    @Test
    void existsByParentIdAndName_true() {
        UUID parentId = UuidV7Utils.generateUuid();
        String name = "existing-file.txt";

        when(repository.existsByParentIdAndName(parentId, name))
            .thenReturn(Mono.just(true));

        StepVerifier.create(service.existsByParentIdAndName(parentId, name))
            .assertNext(exists -> assertThat(exists).isTrue())
            .verifyComplete();
    }

    @Test
    void existsByParentIdAndName_false() {
        UUID parentId = UuidV7Utils.generateUuid();
        String name = "nonexistent-file.txt";

        when(repository.existsByParentIdAndName(parentId, name))
            .thenReturn(Mono.just(false));

        StepVerifier.create(service.existsByParentIdAndName(parentId, name))
            .assertNext(exists -> assertThat(exists).isFalse())
            .verifyComplete();
    }

    @Test
    void existsByParentIdAndName_nullParentId_defaultsToRoot() {
        String name = "test.txt";

        when(repository.existsByParentIdAndName(AttachmentConst.ROOT_DIRECTORY_ID, name))
            .thenReturn(Mono.just(true));

        StepVerifier.create(service.existsByParentIdAndName(null, name))
            .assertNext(exists -> assertThat(exists).isTrue())
            .verifyComplete();
    }

    @Test
    void existsByParentIdAndName_emptyName_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.existsByParentIdAndName(UuidV7Utils.generateUuid(), ""));
    }

    // ===== existsByTypeAndParentIdAndName =====

    @Test
    void existsByTypeAndParentIdAndName_true() {
        UUID parentId = UuidV7Utils.generateUuid();
        String name = "existing-file.txt";

        when(repository.existsByTypeAndParentIdAndName(
            AttachmentType.File, parentId, name))
            .thenReturn(Mono.just(true));

        StepVerifier.create(service.existsByTypeAndParentIdAndName(
                AttachmentType.File, parentId, name))
            .assertNext(exists -> assertThat(exists).isTrue())
            .verifyComplete();
    }

    @Test
    void existsByTypeAndParentIdAndName_false() {
        UUID parentId = UuidV7Utils.generateUuid();
        String name = "nonexistent-file.txt";

        when(repository.existsByTypeAndParentIdAndName(
            AttachmentType.File, parentId, name))
            .thenReturn(Mono.just(false));

        StepVerifier.create(service.existsByTypeAndParentIdAndName(
                AttachmentType.File, parentId, name))
            .assertNext(exists -> assertThat(exists).isFalse())
            .verifyComplete();
    }

    @Test
    void existsByTypeAndParentIdAndName_nullParentId_defaultsToRoot() {
        String name = "test-file.dat";

        when(repository.existsByTypeAndParentIdAndName(
            AttachmentType.File, AttachmentConst.ROOT_DIRECTORY_ID, name))
            .thenReturn(Mono.just(true));

        StepVerifier.create(service.existsByTypeAndParentIdAndName(
                AttachmentType.File, null, name))
            .assertNext(exists -> assertThat(exists).isTrue())
            .verifyComplete();
    }

    @Test
    void existsByTypeAndParentIdAndName_directoryType() {
        UUID parentId = UuidV7Utils.generateUuid();
        String name = "my-folder";

        when(repository.existsByTypeAndParentIdAndName(
            AttachmentType.Directory, parentId, name))
            .thenReturn(Mono.just(true));

        StepVerifier.create(service.existsByTypeAndParentIdAndName(
                AttachmentType.Directory, parentId, name))
            .assertNext(exists -> assertThat(exists).isTrue())
            .verifyComplete();
    }

    @Test
    void existsByTypeAndParentIdAndName_nullType_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.existsByTypeAndParentIdAndName(
                null, UuidV7Utils.generateUuid(), "test.txt"));
    }

    @Test
    void existsByTypeAndParentIdAndName_emptyName_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.existsByTypeAndParentIdAndName(
                AttachmentType.File, UuidV7Utils.generateUuid(), ""));
    }
}
