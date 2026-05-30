package run.ikaros.server.core.binding.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.binding.DirectoryBindingContext;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.core.episode.EpisodeService;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.AttachmentRepository;

class ProcessSpSubdirectoriesStepTest {

    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private EpisodeService episodeService;
    @Mock
    private AttachmentReferenceRepository attachmentReferenceRepository;
    private ProcessSpSubdirectoriesStep step;
    private UUID subjectId;
    private UUID directoryId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        step = new ProcessSpSubdirectoriesStep(
            attachmentRepository, episodeService, attachmentReferenceRepository);
        subjectId = UUID.randomUUID();
        directoryId = UUID.randomUUID();
    }

    @Test
    void execute_processesOpDir() {
        UUID spDirId = UUID.randomUUID();
        Attachment opDir = Attachment.builder()
            .id(spDirId).name("OP").build();
        AttachmentEntity videoFile = AttachmentEntity.builder()
            .id(UUID.randomUUID()).name("OP.mkv")
            .type(AttachmentType.File).parentId(spDirId).build();

        when(attachmentRepository.findAllByParentId(spDirId))
            .thenReturn(Flux.just(videoFile));

        Episode savedEpisode = Episode.builder()
            .id(UUID.randomUUID()).subjectId(subjectId)
            .name("OP").sequence(1f).group(EpisodeGroup.OPENING_SONG).build();

        when(episodeService.save(any(Episode.class)))
            .thenReturn(Mono.just(savedEpisode));
        when(attachmentReferenceRepository.insert(any(AttachmentReferenceEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test Anime", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setSpSubdirectoryAttachments(List.of(opDir));

        StepVerifier.create(step.execute(context))
            .assertNext(ctx -> {
                assertThat(ctx.getCreatedEpisodes()).hasSize(1);
                assertThat(ctx.getCreatedAttachmentRefs()).hasSize(1);
                assertThat(ctx.getCreatedEpisodes().get(0).getGroup())
                    .isEqualTo(EpisodeGroup.OPENING_SONG);
            })
            .verifyComplete();
    }

    @Test
    void execute_mapsDirectoryNamesToCorrectGroups() {
        UUID spDirId1 = UUID.randomUUID();
        UUID spDirId2 = UUID.randomUUID();
        Attachment opDir = Attachment.builder().id(spDirId1).name("OP").build();
        Attachment edDir = Attachment.builder().id(spDirId2).name("ED").build();

        AttachmentEntity opFile = AttachmentEntity.builder()
            .id(UUID.randomUUID()).name("OP.mkv")
            .type(AttachmentType.File).parentId(spDirId1).build();
        AttachmentEntity edFile = AttachmentEntity.builder()
            .id(UUID.randomUUID()).name("ED.mkv")
            .type(AttachmentType.File).parentId(spDirId2).build();

        when(attachmentRepository.findAllByParentId(spDirId1)).thenReturn(Flux.just(opFile));
        when(attachmentRepository.findAllByParentId(spDirId2)).thenReturn(Flux.just(edFile));

        when(episodeService.save(any(Episode.class)))
            .thenReturn(Mono.just(Episode.builder()
                .id(UUID.randomUUID()).subjectId(subjectId)
                .name("OP").sequence(1f).group(EpisodeGroup.OPENING_SONG).build()))
            .thenReturn(Mono.just(Episode.builder()
                .id(UUID.randomUUID()).subjectId(subjectId)
                .name("ED").sequence(1f).group(EpisodeGroup.ENDING_SONG).build()));
        when(attachmentReferenceRepository.insert(any(AttachmentReferenceEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test Anime", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setSpSubdirectoryAttachments(List.of(opDir, edDir));

        StepVerifier.create(step.execute(context))
            .assertNext(ctx -> {
                assertThat(ctx.getCreatedEpisodes()).hasSize(2);
                assertThat(ctx.getCreatedEpisodes())
                    .extracting(run.ikaros.api.core.subject.Episode::getGroup)
                    .containsExactlyInAnyOrder(
                        EpisodeGroup.OPENING_SONG, EpisodeGroup.ENDING_SONG);
            })
            .verifyComplete();
    }

    @Test
    void execute_handlesOvaAndOadDirs() {
        UUID spDirId = UUID.randomUUID();
        Attachment ovaDir = Attachment.builder().id(spDirId).name("OVA").build();
        AttachmentEntity ovaFile = AttachmentEntity.builder()
            .id(UUID.randomUUID()).name("OVA.mkv")
            .type(AttachmentType.File).parentId(spDirId).build();

        when(attachmentRepository.findAllByParentId(spDirId)).thenReturn(Flux.just(ovaFile));

        when(episodeService.save(any(Episode.class)))
            .thenReturn(Mono.just(Episode.builder()
                .id(UUID.randomUUID()).subjectId(subjectId)
                .name("OVA").sequence(1f)
                .group(EpisodeGroup.ORIGINAL_VIDEO_ANIMATION).build()));
        when(attachmentReferenceRepository.insert(any(AttachmentReferenceEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setSpSubdirectoryAttachments(List.of(ovaDir));

        StepVerifier.create(step.execute(context))
            .assertNext(ctx ->
                assertThat(ctx.getCreatedEpisodes().get(0).getGroup())
                    .isEqualTo(EpisodeGroup.ORIGINAL_VIDEO_ANIMATION)
            )
            .verifyComplete();
    }

    @Test
    void execute_handlesDefaultCase() {
        UUID spDirId = UUID.randomUUID();
        Attachment miscDir = Attachment.builder().id(spDirId).name("SPECIAL").build();
        AttachmentEntity miscFile = AttachmentEntity.builder()
            .id(UUID.randomUUID()).name("special.mkv")
            .type(AttachmentType.File).parentId(spDirId).build();

        when(attachmentRepository.findAllByParentId(spDirId)).thenReturn(Flux.just(miscFile));

        when(episodeService.save(any(Episode.class)))
            .thenReturn(Mono.just(Episode.builder()
                .id(UUID.randomUUID()).subjectId(subjectId)
                .name("special").sequence(1f)
                .group(EpisodeGroup.SPECIAL_PROMOTION).build()));
        when(attachmentReferenceRepository.insert(any(AttachmentReferenceEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setSpSubdirectoryAttachments(List.of(miscDir));

        StepVerifier.create(step.execute(context))
            .assertNext(ctx ->
                assertThat(ctx.getCreatedEpisodes().get(0).getGroup())
                    .isIn(EpisodeGroup.SPECIAL_PROMOTION, EpisodeGroup.OTHER)
            )
            .verifyComplete();
    }

    @Test
    void execute_emptySpDirectory_createsNoEpisodes() {
        UUID spDirId = UUID.randomUUID();
        Attachment spDir = Attachment.builder().id(spDirId).name("OP").build();

        when(attachmentRepository.findAllByParentId(spDirId)).thenReturn(Flux.empty());

        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setSpSubdirectoryAttachments(List.of(spDir));

        StepVerifier.create(step.execute(context))
            .assertNext(ctx -> {
                assertThat(ctx.getCreatedEpisodes()).isEmpty();
                assertThat(ctx.getCreatedAttachmentRefs()).isEmpty();
            })
            .verifyComplete();
    }

    @Test
    void shouldSkip_whenSpSubdirectoryAttachmentsIsNull() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setSpSubdirectoryAttachments(null);
        assertThat(step.shouldSkip(context)).isTrue();
    }

    @Test
    void shouldSkip_whenSpSubdirectoryAttachmentsIsEmpty() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setSpSubdirectoryAttachments(List.of());
        assertThat(step.shouldSkip(context)).isTrue();
    }

    @Test
    void shouldSkip_whenSubjectIdIsNull() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(null);
        context.setSpSubdirectoryAttachments(List.of(
            Attachment.builder().id(UUID.randomUUID()).name("OP").build()));
        assertThat(step.shouldSkip(context)).isTrue();
    }

    @Test
    void rollback_removesRefsAndEpisodes() {
        UUID refId = UUID.randomUUID();
        UUID epId = UUID.randomUUID();
        run.ikaros.api.core.attachment.AttachmentReference ref =
            run.ikaros.api.core.attachment.AttachmentReference.builder()
                .id(refId).attachmentId(UUID.randomUUID()).referenceId(epId).build();
        Episode episode = Episode.builder().id(epId).build();

        when(attachmentReferenceRepository.deleteById(refId)).thenReturn(Mono.empty());
        when(episodeService.deleteById(epId)).thenReturn(Mono.empty());

        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test", SubjectSyncPlatform.BGM_TV);
        context.getCreatedAttachmentRefs().add(ref);
        context.getCreatedEpisodes().add(episode);

        StepVerifier.create(step.rollback(context)).verifyComplete();
        verify(attachmentReferenceRepository).deleteById(refId);
        verify(episodeService).deleteById(epId);
    }

    @Test
    void rollback_error_doesNotPropagate() {
        UUID refId = UUID.randomUUID();
        run.ikaros.api.core.attachment.AttachmentReference ref =
            run.ikaros.api.core.attachment.AttachmentReference.builder()
                .id(refId).build();

        when(attachmentReferenceRepository.deleteById(refId))
            .thenReturn(Mono.error(new RuntimeException("Delete failed")));

        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test", SubjectSyncPlatform.BGM_TV);
        context.getCreatedAttachmentRefs().add(ref);

        StepVerifier.create(step.rollback(context)).verifyComplete();
    }

    @Test
    void order_is75() {
        assertThat(step.order()).isEqualTo(75);
    }

    @Test
    void name_isProcessSpSubdirectories() {
        assertThat(step.name()).isEqualTo("ProcessSpSubdirectories");
    }
}
