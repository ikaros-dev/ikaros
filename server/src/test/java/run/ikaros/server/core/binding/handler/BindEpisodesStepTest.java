package run.ikaros.server.core.binding.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.binding.DirectoryBindingContext;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.core.episode.EpisodeService;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;
import run.ikaros.server.store.entity.EpisodeEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.EpisodeRepository;

class BindEpisodesStepTest {

    @Mock
    private EpisodeRepository episodeRepository;
    @Mock
    private EpisodeService episodeService;
    @Mock
    private AttachmentReferenceRepository attachmentReferenceRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    private BindEpisodesStep step;
    private UUID subjectId;
    private UUID directoryId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        step = new BindEpisodesStep(
            episodeRepository, episodeService,
            attachmentReferenceRepository, eventPublisher);
        subjectId = UUID.randomUUID();
        directoryId = UUID.randomUUID();
    }

    @Test
    void execute_bindsVideoToEpisode() {
        // Use filename format supported by RegexUtils.parseEpisodeSeqByFileName
        Attachment video = Attachment.builder()
            .id(UUID.randomUUID()).name("[01].mkv").build();

        when(episodeRepository.findBySubjectIdAndGroupAndSequence(
            any(), any(), any()))
            .thenReturn(Flux.empty());
        when(episodeService.save(any(Episode.class)))
            .thenReturn(Mono.just(Episode.builder()
                .id(UUID.randomUUID()).subjectId(subjectId)
                .name("Episode 1").sequence(1f).group(EpisodeGroup.MAIN).build()));
        when(attachmentReferenceRepository.insert(any(AttachmentReferenceEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test Anime", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setChildAttachments(List.of(video));

        StepVerifier.create(step.execute(context))
            .assertNext(ctx -> {
                // Episode created and bound to file
                assertThat(ctx.getCreatedEpisodes()).hasSize(1);
                assertThat(ctx.getCreatedAttachmentRefs()).hasSize(1);
            })
            .verifyComplete();

        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void execute_createsNewEpisodeWhenNotFound() {
        Attachment video = Attachment.builder()
            .id(UUID.randomUUID()).name("[01].mkv").build();
        Episode savedEpisode = Episode.builder()
            .id(UUID.randomUUID()).subjectId(subjectId)
            .name("Episode 1").sequence(1f).group(EpisodeGroup.MAIN).build();

        when(episodeRepository.findBySubjectIdAndGroupAndSequence(
            any(), any(), anyFloat()))
            .thenReturn(Flux.empty());
        when(episodeService.save(any(Episode.class)))
            .thenReturn(Mono.just(savedEpisode));
        when(attachmentReferenceRepository.insert(any(AttachmentReferenceEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test Anime", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setChildAttachments(List.of(video));

        StepVerifier.create(step.execute(context))
            .assertNext(ctx -> {
                assertThat(ctx.getCreatedEpisodes()).hasSize(1);
                assertThat(ctx.getCreatedAttachmentRefs()).hasSize(1);
            })
            .verifyComplete();
    }

    @Test
    void execute_skipsFileWithUnparseableSequence() {
        Attachment video = Attachment.builder()
            .id(UUID.randomUUID()).name("cover.jpg").build();

        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test Anime", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setChildAttachments(List.of(video));

        StepVerifier.create(step.execute(context))
            .assertNext(ctx -> {
                assertThat(ctx.getCreatedEpisodes()).isEmpty();
                assertThat(ctx.getCreatedAttachmentRefs()).isEmpty();
            })
            .verifyComplete();
    }

    @Test
    void execute_bindsMultipleFiles() {
        Attachment video1 = Attachment.builder()
            .id(UUID.randomUUID()).name("[01].mkv").build();
        Attachment video2 = Attachment.builder()
            .id(UUID.randomUUID()).name("[02].mkv").build();

        Episode savedEp1 = Episode.builder()
            .id(UUID.randomUUID()).subjectId(subjectId)
            .name("Episode 1").sequence(1f).group(EpisodeGroup.MAIN).build();
        Episode savedEp2 = Episode.builder()
            .id(UUID.randomUUID()).subjectId(subjectId)
            .name("Episode 2").sequence(2f).group(EpisodeGroup.MAIN).build();

        // findAllBy returns empty, then save is called for each new episode
        when(episodeRepository.findBySubjectIdAndGroupAndSequence(
            any(), any(), anyFloat()))
            .thenReturn(Flux.empty());
        when(episodeService.save(any(Episode.class)))
            .thenReturn(Mono.just(savedEp1))
            .thenReturn(Mono.just(savedEp2));
        when(attachmentReferenceRepository.insert(any(AttachmentReferenceEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test Anime", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setChildAttachments(List.of(video1, video2));

        StepVerifier.create(step.execute(context))
            .assertNext(ctx -> {
                assertThat(ctx.getCreatedEpisodes()).hasSize(2);
                assertThat(ctx.getCreatedAttachmentRefs()).hasSize(2);
            })
            .verifyComplete();
    }

    @Test
    void shouldSkip_whenChildAttachmentsIsNull() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setChildAttachments(null);
        assertThat(step.shouldSkip(context)).isTrue();
    }

    @Test
    void shouldSkip_whenChildAttachmentsIsEmpty() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setChildAttachments(List.of());
        assertThat(step.shouldSkip(context)).isTrue();
    }

    @Test
    void shouldSkip_whenSubjectIdIsNull() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(null);
        context.setChildAttachments(List.of(
            Attachment.builder().id(UUID.randomUUID()).name("EP01.mkv").build()));
        assertThat(step.shouldSkip(context)).isTrue();
    }

    @Test
    void shouldSkip_whenAllConditionsMet_returnsFalse() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setChildAttachments(List.of(
            Attachment.builder().id(UUID.randomUUID()).name("EP01.mkv").build()));
        assertThat(step.shouldSkip(context)).isFalse();
    }

    @Test
    void rollback_removesRefsAndEpisodes() {
        UUID refId = UUID.randomUUID();
        UUID epId = UUID.randomUUID();
        run.ikaros.api.core.attachment.AttachmentReference ref =
            run.ikaros.api.core.attachment.AttachmentReference.builder()
                .id(refId).type(AttachmentReferenceType.EPISODE)
                .attachmentId(UUID.randomUUID()).referenceId(epId).build();
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
                .id(refId).type(AttachmentReferenceType.EPISODE)
                .attachmentId(UUID.randomUUID()).referenceId(UUID.randomUUID()).build();

        when(attachmentReferenceRepository.deleteById(refId))
            .thenReturn(Mono.error(new RuntimeException("Delete failed")));

        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test", SubjectSyncPlatform.BGM_TV);
        context.getCreatedAttachmentRefs().add(ref);

        StepVerifier.create(step.rollback(context)).verifyComplete();
    }

    @Test
    void order_is70() {
        assertThat(step.order()).isEqualTo(70);
    }

    @Test
    void name_isBindEpisodes() {
        assertThat(step.name()).isEqualTo("BindEpisodes");
    }
}
