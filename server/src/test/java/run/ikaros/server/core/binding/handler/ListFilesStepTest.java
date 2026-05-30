package run.ikaros.server.core.binding.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import run.ikaros.api.core.binding.DirectoryBindingContext;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentRepository;

class ListFilesStepTest {

    @Mock
    private AttachmentRepository attachmentRepository;
    private ListFilesStep step;
    private UUID directoryId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        step = new ListFilesStep(attachmentRepository);
        directoryId = UUID.randomUUID();
    }

    @Test
    void execute_filtersVideoFilesAndSpDirectories() {
        AttachmentEntity video1 = AttachmentEntity.builder()
            .id(UUID.randomUUID()).parentId(directoryId)
            .name("EP01.mkv").type(AttachmentType.File).build();
        AttachmentEntity video2 = AttachmentEntity.builder()
            .id(UUID.randomUUID()).parentId(directoryId)
            .name("EP02.mp4").type(AttachmentType.File).build();
        AttachmentEntity spDir = AttachmentEntity.builder()
            .id(UUID.randomUUID()).parentId(directoryId)
            .name("SP").type(AttachmentType.Directory).build();
        AttachmentEntity nonVideo = AttachmentEntity.builder()
            .id(UUID.randomUUID()).parentId(directoryId)
            .name("cover.jpg").type(AttachmentType.File).build();
        AttachmentEntity normalDir = AttachmentEntity.builder()
            .id(UUID.randomUUID()).parentId(directoryId)
            .name("Subs").type(AttachmentType.Directory).build();

        when(attachmentRepository.findAllByParentId(directoryId))
            .thenReturn(Flux.just(video1, video2, spDir, nonVideo, normalDir));

        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test Anime", SubjectSyncPlatform.BGM_TV);

        StepVerifier.create(step.execute(context))
            .assertNext(ctx -> {
                assertThat(ctx.getChildAttachments()).hasSize(2);
                assertThat(ctx.getChildAttachments())
                    .extracting("name")
                    .containsExactly("EP01.mkv", "EP02.mp4");
                assertThat(ctx.getSpSubdirectoryAttachments()).hasSize(1);
                assertThat(ctx.getSpSubdirectoryAttachments())
                    .extracting("name")
                    .containsExactly("SP");
            })
            .verifyComplete();
    }

    @Test
    void execute_emptyDirectory() {
        when(attachmentRepository.findAllByParentId(directoryId))
            .thenReturn(Flux.empty());

        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Empty Dir", SubjectSyncPlatform.BGM_TV);

        StepVerifier.create(step.execute(context))
            .assertNext(ctx -> {
                assertThat(ctx.getChildAttachments()).isEmpty();
                assertThat(ctx.getSpSubdirectoryAttachments()).isEmpty();
            })
            .verifyComplete();
    }

    @Test
    void execute_recognizesAllSpVariants() {
        AttachmentEntity opDir = AttachmentEntity.builder()
            .id(UUID.randomUUID()).parentId(directoryId)
            .name("OP").type(AttachmentType.Directory).build();
        AttachmentEntity edDir = AttachmentEntity.builder()
            .id(UUID.randomUUID()).parentId(directoryId)
            .name("ED").type(AttachmentType.Directory).build();
        AttachmentEntity ovaDir = AttachmentEntity.builder()
            .id(UUID.randomUUID()).parentId(directoryId)
            .name("OVA").type(AttachmentType.Directory).build();
        AttachmentEntity oadDir = AttachmentEntity.builder()
            .id(UUID.randomUUID()).parentId(directoryId)
            .name("OAD").type(AttachmentType.Directory).build();
        AttachmentEntity specialDir = AttachmentEntity.builder()
            .id(UUID.randomUUID()).parentId(directoryId)
            .name("SPECIAL").type(AttachmentType.Directory).build();
        AttachmentEntity normalDir = AttachmentEntity.builder()
            .id(UUID.randomUUID()).parentId(directoryId)
            .name("Others").type(AttachmentType.Directory).build();

        when(attachmentRepository.findAllByParentId(directoryId))
            .thenReturn(Flux.just(opDir, edDir, ovaDir, oadDir, specialDir, normalDir));

        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test", SubjectSyncPlatform.BGM_TV);

        StepVerifier.create(step.execute(context))
            .assertNext(ctx ->
                assertThat(ctx.getSpSubdirectoryAttachments())
                    .extracting("name")
                    .containsExactlyInAnyOrder("OP", "ED", "OVA", "OAD", "SPECIAL")
            )
            .verifyComplete();
    }

    @Test
    void execute_driverTypeAttachmentsAreAlsoIncluded() {
        AttachmentEntity driverVideo = AttachmentEntity.builder()
            .id(UUID.randomUUID()).parentId(directoryId)
            .name("EP01.mkv").type(AttachmentType.Driver_File).build();
        AttachmentEntity driverDir = AttachmentEntity.builder()
            .id(UUID.randomUUID()).parentId(directoryId)
            .name("OP").type(AttachmentType.Driver_Directory).build();

        when(attachmentRepository.findAllByParentId(directoryId))
            .thenReturn(Flux.just(driverVideo, driverDir));

        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test", SubjectSyncPlatform.BGM_TV);

        StepVerifier.create(step.execute(context))
            .assertNext(ctx -> {
                assertThat(ctx.getChildAttachments()).hasSize(1);
                assertThat(ctx.getSpSubdirectoryAttachments()).hasSize(1);
            })
            .verifyComplete();
    }

    @Test
    void order_is50() {
        assertThat(step.order()).isEqualTo(50);
    }

    @Test
    void name_isListFiles() {
        assertThat(step.name()).isEqualTo("ListFiles");
    }

    @Test
    void rollback_doesNothing() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test", SubjectSyncPlatform.BGM_TV);
        StepVerifier.create(step.rollback(context)).verifyComplete();
    }
}
