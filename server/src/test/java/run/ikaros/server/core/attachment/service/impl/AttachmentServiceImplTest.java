package run.ikaros.server.core.attachment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentConst;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.core.attachment.AttachmentUploadCondition;
import run.ikaros.api.core.attachment.exception.AttachmentUploadException;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.core.attachment.service.AttachmentMediaValidationService;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.entity.AttachmentDriverEntity;
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
    @Mock
    private AttachmentMediaValidationService mediaValidationService;
    private AttachmentServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AttachmentServiceImpl(
            repository, referenceRepository, relationRepository,
            template, ikarosProperties, applicationEventPublisher,
            attachmentRepository, driverRepository, extensionComponentsFinder,
            mediaValidationService);
    }

    @Test
    void constructor_withNineParams() {
        assertThat(service).isNotNull();
    }

    @Test
    void upload_rejectsFilenameBeforeSubscribingContent() {
        int[] subscriptions = {0};
        when(mediaValidationService.validateFilename("payload.exe"))
            .thenThrow(new IllegalArgumentException("不支持的媒体文件名"));

        AttachmentUploadCondition condition = AttachmentUploadCondition
            .builder()
            .name("payload.exe")
            .dataBufferFlux(Flux.defer(() -> {
                subscriptions[0]++;
                return Flux.empty();
            }))
            .build();

        assertThatThrownBy(() -> service.upload(condition))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(subscriptions[0]).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    void upload_rejectsSystemRootDirectory() {
        AttachmentUploadCondition condition = AttachmentUploadCondition
            .builder()
            .name("video.mp4")
            .parentId(AttachmentConst.ROOT_DIRECTORY_ID)
            .dataBufferFlux(Flux.empty())
            .build();

        StepVerifier.create(service.upload(condition))
            .expectErrorSatisfies(error -> assertThat(error)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("根目录只能包含文件夹"))
            .verify();
    }

    @Test
    void save_rejectsMovingFileIntoDriverDirectory() {
        UUID driverDirectoryId = UUID.randomUUID();
        Attachment file = Attachment
            .builder()
            .id(UUID.randomUUID())
            .name("video.mp4")
            .type(AttachmentType.File)
            .parentId(driverDirectoryId)
            .build();
        when(repository.findById(driverDirectoryId)).thenReturn(Mono.just(AttachmentEntity
            .builder()
            .id(driverDirectoryId)
            .type(AttachmentType.Driver_Directory)
            .build()));

        StepVerifier.create(service.save(file))
            .expectErrorSatisfies(error -> assertThat(error)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("文件源目录不允许上传或移动文件"))
            .verify();
    }

    @Test
    void save_allowsDriverDirectoryOutsideWorkDirectory(@TempDir Path tempDir) {
        Path workDirectory = tempDir.resolve("work");
        Path driverDirectory = tempDir.resolve("driver");
        Attachment attachment = Attachment
            .builder()
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

        StepVerifier
            .create(service.save(attachment))
            .assertNext(saved -> {
                assertThat(saved.getFsPath()).isEqualTo(driverDirectory.toString());
                assertThat(saved.getDriverId()).isEqualTo(attachment.getDriverId());
            })
            .verifyComplete();
    }

    @Test
    void save_rejectsRegularAttachmentOutsideWorkDirectory(@TempDir Path tempDir) {
        Path workDirectory = tempDir.resolve("work");
        Path outsideFile = tempDir.resolve("outside.mkv");
        Attachment attachment = Attachment
            .builder()
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
        Attachment attachment = Attachment
            .builder()
            .name("driver")
            .type(AttachmentType.Driver_Directory)
            .fsPath(tempDir.toString())
            .build();

        assertThatThrownBy(() -> service.save(attachment))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("driverId");
    }

    @Test
    void getStreamById_usesDetectedMimeForMislabeledImage(@TempDir Path tempDir)
        throws Exception {
        Path file = tempDir.resolve("cover.png");
        byte[] content = jpeg();
        java.nio.file.Files.write(file, content);
        when(ikarosProperties.getWorkDir()).thenReturn(tempDir);
        UUID id = UUID.randomUUID();
        AttachmentEntity entity = AttachmentEntity
            .builder()
            .id(id)
            .name("cover.png")
            .type(AttachmentType.File)
            .fsPath(file.toString())
            .url(file.toString())
            .size((long) content.length)
            .build();
        when(repository.findById(id)).thenReturn(Mono.just(entity));
        service = newServiceWithRealValidation();

        StepVerifier
            .create(service.getStreamById(id))
            .assertNext(stream -> {
                assertThat(stream.getContextType()).isEqualTo("image/jpeg");
                assertThat(read(stream)).isEqualTo(content);
            })
            .verifyComplete();
    }

    @Test
    void getStreamByIdWithRange_validatesFullPrefixBeforeReadingRange(@TempDir Path tempDir)
        throws Exception {
        Path file = tempDir.resolve("song.mp4");
        byte[] content = mp3();
        java.nio.file.Files.write(file, content);
        when(ikarosProperties.getWorkDir()).thenReturn(tempDir);
        UUID id = UUID.randomUUID();
        AttachmentEntity entity = AttachmentEntity
            .builder()
            .id(id)
            .name("song.mp4")
            .type(AttachmentType.File)
            .fsPath(file.toString())
            .url(file.toString())
            .size((long) content.length)
            .build();
        when(repository.findById(id)).thenReturn(Mono.just(entity));
        service = newServiceWithRealValidation();

        StepVerifier
            .create(service.getStreamByIdWithRange(id, 2, 5))
            .assertNext(stream -> {
                assertThat(stream.getContextType()).isEqualTo("audio/mpeg");
                assertThat(read(stream)).isEqualTo(java.util.Arrays.copyOfRange(content, 2, 6));
            })
            .verifyComplete();
    }

    @Test
    void getStreamById_validatesAndReopensDriverStream() {
        byte[] content = jpeg();
        UUID id = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        AttachmentEntity entity = AttachmentEntity
            .builder()
            .id(id)
            .driverId(driverId)
            .name("cover.png")
            .type(AttachmentType.Driver_File)
            .fsPath("driver-file")
            .size((long) content.length)
            .build();
        AttachmentDriverEntity driver = AttachmentDriverEntity
            .builder()
            .id(driverId)
            .type(AttachmentDriverType.LOCAL)
            .name("DISK")
            .build();
        AttachmentDriverFetcher fetcher = org.mockito.Mockito.mock(
            AttachmentDriverFetcher.class);
        int[] subscriptions = {0};
        when(repository.findById(id)).thenReturn(Mono.just(entity));
        when(driverRepository.findById(driverId)).thenReturn(Mono.just(driver));
        when(extensionComponentsFinder.getExtensions(AttachmentDriverFetcher.class))
            .thenReturn(java.util.List.of(fetcher));
        when(fetcher.getDriverType()).thenReturn(AttachmentDriverType.LOCAL);
        when(fetcher.getDriverName()).thenReturn("DISK");
        when(fetcher.getSteam(any(Attachment.class))).thenAnswer(invocation -> Flux.defer(() -> {
            subscriptions[0]++;
            return Flux.just(org.springframework.core.io.buffer.DefaultDataBufferFactory
                .sharedInstance.wrap(content));
        }));
        service = newServiceWithRealValidation();

        StepVerifier
            .create(service.getStreamById(id))
            .assertNext(stream -> {
                assertThat(stream.getContextType()).isEqualTo("image/jpeg");
                assertThat(read(stream)).isEqualTo(content);
            })
            .verifyComplete();
        assertThat(subscriptions[0]).isEqualTo(2);
    }

    @Test
    void getStreamById_rejectsUnsupportedNameBeforeOpeningFile(@TempDir Path tempDir) {
        UUID id = UUID.randomUUID();
        AttachmentEntity entity = AttachmentEntity
            .builder()
            .id(id)
            .name("payload.exe")
            .type(AttachmentType.File)
            .fsPath(tempDir
                .resolve("payload.exe")
                .toString())
            .size(10L)
            .build();
        when(repository.findById(id)).thenReturn(Mono.just(entity));
        when(mediaValidationService.validateFilename("payload.exe"))
            .thenThrow(new IllegalArgumentException("unsupported"));

        StepVerifier
            .create(service.getStreamById(id))
            .expectErrorSatisfies(error -> assertThat(error)
                .isInstanceOf(AttachmentUploadException.class))
            .verify();
        verify(driverRepository, never()).findById(any(UUID.class));
    }

    private AttachmentServiceImpl newServiceWithRealValidation() {
        return new AttachmentServiceImpl(
            repository, referenceRepository, relationRepository, template,
            ikarosProperties, applicationEventPublisher, attachmentRepository,
            driverRepository, extensionComponentsFinder,
            new DefaultAttachmentMediaValidationService());
    }

    private byte[] read(run.ikaros.api.core.attachment.AttachmentStreamVo stream) {
        return org.springframework.core.io.buffer.DataBufferUtils
            .join(stream.getDataBufferFlux())
            .map(buffer -> {
                byte[] bytes = new byte[buffer.readableByteCount()];
                buffer.read(bytes);
                org.springframework.core.io.buffer.DataBufferUtils.release(buffer);
                return bytes;
            })
            .block();
    }

    private byte[] jpeg() {
        return hex("ffd8ffc00011080001000103011100021100031100");
    }

    private byte[] mp3() {
        byte[] data = new byte[417];
        data[0] = (byte) 0xff;
        data[1] = (byte) 0xfb;
        data[2] = (byte) 0x90;
        data[3] = 0x64;
        return data;
    }

    private byte[] hex(String value) {
        byte[] bytes = new byte[value.length() / 2];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = (byte) Integer.parseInt(value.substring(index * 2, index * 2 + 2), 16);
        }
        return bytes;
    }

    // ===== findById =====

    @Test
    void findById_found() {
        UUID id = UuidV7Utils.generateUuid();
        AttachmentEntity entity = AttachmentEntity
            .builder()
            .id(id)
            .name("test-file.mp4")
            .type(AttachmentType.File)
            .parentId(AttachmentConst.ROOT_DIRECTORY_ID)
            .size(1024L)
            .build();

        when(repository.findById(id)).thenReturn(Mono.just(entity));

        StepVerifier
            .create(service.findById(id))
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

        StepVerifier
            .create(service.findById(id))
            .verifyComplete();
    }

    // ===== findByTypeAndParentIdAndName =====

    @Test
    void findByTypeAndParentIdAndName_found() {
        UUID parentId = UuidV7Utils.generateUuid();
        String name = "test-video.mkv";

        AttachmentEntity entity = AttachmentEntity
            .builder()
            .id(UuidV7Utils.generateUuid())
            .name(name)
            .type(AttachmentType.File)
            .parentId(parentId)
            .size(2048L)
            .build();

        when(repository.findByTypeAndParentIdAndName(AttachmentType.File, parentId, name))
            .thenReturn(Mono.just(entity));

        StepVerifier
            .create(service.findByTypeAndParentIdAndName(
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

        StepVerifier
            .create(service.findByTypeAndParentIdAndName(
                AttachmentType.File, parentId, name))
            .verifyComplete();
    }

    @Test
    void findByTypeAndParentIdAndName_nullParentId_defaultsToRoot() {
        String name = "test.txt";

        AttachmentEntity entity = AttachmentEntity
            .builder()
            .id(UuidV7Utils.generateUuid())
            .name(name)
            .type(AttachmentType.File)
            .parentId(AttachmentConst.ROOT_DIRECTORY_ID)
            .size(100L)
            .build();

        when(repository.findByTypeAndParentIdAndName(
            AttachmentType.File, AttachmentConst.ROOT_DIRECTORY_ID, name))
            .thenReturn(Mono.just(entity));

        StepVerifier
            .create(service.findByTypeAndParentIdAndName(
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

        StepVerifier
            .create(service.existsByParentIdAndName(parentId, name))
            .assertNext(exists -> assertThat(exists).isTrue())
            .verifyComplete();
    }

    @Test
    void existsByParentIdAndName_false() {
        UUID parentId = UuidV7Utils.generateUuid();
        String name = "nonexistent-file.txt";

        when(repository.existsByParentIdAndName(parentId, name))
            .thenReturn(Mono.just(false));

        StepVerifier
            .create(service.existsByParentIdAndName(parentId, name))
            .assertNext(exists -> assertThat(exists).isFalse())
            .verifyComplete();
    }

    @Test
    void existsByParentIdAndName_nullParentId_defaultsToRoot() {
        String name = "test.txt";

        when(repository.existsByParentIdAndName(AttachmentConst.ROOT_DIRECTORY_ID, name))
            .thenReturn(Mono.just(true));

        StepVerifier
            .create(service.existsByParentIdAndName(null, name))
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

        StepVerifier
            .create(service.existsByTypeAndParentIdAndName(
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

        StepVerifier
            .create(service.existsByTypeAndParentIdAndName(
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

        StepVerifier
            .create(service.existsByTypeAndParentIdAndName(
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

        StepVerifier
            .create(service.existsByTypeAndParentIdAndName(
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
