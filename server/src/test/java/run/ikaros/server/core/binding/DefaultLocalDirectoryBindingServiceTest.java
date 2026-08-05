package run.ikaros.server.core.binding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentReference;
import run.ikaros.api.core.binding.LocalMediaMode;
import run.ikaros.api.core.binding.LocalScanAssignment;
import run.ikaros.api.core.binding.LocalScanConfirmRequest;
import run.ikaros.api.core.binding.LocalScanItem;
import run.ikaros.api.core.binding.LocalScanPreview;
import run.ikaros.api.core.binding.LocalScanPreviewRequest;
import run.ikaros.api.core.binding.MediaPhysicalType;
import run.ikaros.api.core.binding.MediaRole;
import run.ikaros.api.core.binding.MediaTrack;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.server.core.attachment.service.AttachmentReferenceService;
import run.ikaros.server.core.episode.EpisodeService;
import run.ikaros.server.core.subject.service.SubjectService;
import run.ikaros.server.core.task.TaskService;
import run.ikaros.server.store.entity.DirectoryBindingWorkflowEntity;
import run.ikaros.server.store.repository.DirectoryBindingWorkflowRepository;
import run.ikaros.server.store.repository.TaskRepository;

/** 本地目录绑定确认与重扫的幂等性测试。 */
class DefaultLocalDirectoryBindingServiceTest {
    @Mock
    private LocalMediaScanner localMediaScanner;
    @Mock
    private SubjectService subjectService;
    @Mock
    private EpisodeService episodeService;
    @Mock
    private AttachmentReferenceService attachmentReferenceService;
    @Mock
    private TaskService taskService;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private DirectoryBindingWorkflowRepository workflowRepository;

    private LocalDirectoryBindingService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DefaultLocalDirectoryBindingService(localMediaScanner, subjectService, episodeService,
            attachmentReferenceService, taskService, taskRepository, workflowRepository);
    }

    @Test
    void previewShouldDelegateToScanner() {
        LocalScanPreviewRequest request = LocalScanPreviewRequest.builder()
            .directoryId(UUID.randomUUID()).mode(LocalMediaMode.EPISODE).build();
        LocalScanPreview preview = LocalScanPreview.builder().directoryId(request.getDirectoryId())
            .mode(LocalMediaMode.EPISODE).items(List.of()).build();
        when(localMediaScanner.scan(request)).thenReturn(Mono.just(preview));

        assertThat(service.preview(request).block()).isSameAs(preview);

        verify(localMediaScanner).scan(request);
    }

    @Test
    void confirmShouldCreateOnceAndKeepManualAssignmentDuringRescan() {
        UUID directoryId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        UUID subtitleId = UUID.randomUUID();
        UUID episodeId = UUID.randomUUID();
        LocalScanPreview preview = preview(directoryId, videoId, subtitleId);
        LocalScanPreview emptyPreview = LocalScanPreview.builder().directoryId(directoryId)
            .mode(LocalMediaMode.EPISODE).items(List.of()).build();
        AtomicReference<DirectoryBindingWorkflowEntity> storedWorkflow = new AtomicReference<>();
        AttachmentReference reference = AttachmentReference.builder().type(AttachmentReferenceType.EPISODE)
            .attachmentId(videoId).referenceId(episodeId).build();
        LocalScanConfirmRequest request = LocalScanConfirmRequest.builder()
            .directoryId(directoryId).mode(LocalMediaMode.EPISODE).subjectId(subjectId)
            .assignments(List.of(LocalScanAssignment.builder().attachmentId(subtitleId)
                .primaryAttachmentId(videoId).build()))
            .build();

        when(localMediaScanner.scan(any())).thenReturn(Mono.just(preview), Mono.just(preview),
            Mono.just(emptyPreview));
        when(workflowRepository.findLocalWorkflow(directoryId, subjectId, LocalMediaMode.EPISODE.name()))
            .thenAnswer(invocation -> Mono.justOrEmpty(storedWorkflow.get()));
        when(workflowRepository.insert(any())).thenAnswer(invocation -> {
            DirectoryBindingWorkflowEntity entity = invocation.getArgument(0);
            storedWorkflow.set(entity);
            return Mono.just(entity);
        });
        when(workflowRepository.update(any())).thenAnswer(invocation -> {
            DirectoryBindingWorkflowEntity entity = invocation.getArgument(0);
            storedWorkflow.set(entity);
            return Mono.just(entity);
        });
        when(attachmentReferenceService.findAllByTypeAndAttachmentId(AttachmentReferenceType.EPISODE,
            videoId)).thenReturn(Flux.empty(), Flux.just(reference));
        when(episodeService.save(any())).thenReturn(Mono.just(Episode.defaultEpisode(subjectId).setId(episodeId)));
        when(attachmentReferenceService.save(any())).thenReturn(Mono.just(reference));
        when(taskService.submit(any())).thenReturn(Mono.empty());

        DirectoryBindingWorkflowEntity confirmed = service.confirm(request).block();
        DirectoryBindingWorkflowEntity repeated = service.confirm(request).block();
        DirectoryBindingWorkflowEntity rescanned = service.rescan(directoryId, subjectId,
            LocalMediaMode.EPISODE).block();

        assertThat(confirmed.getPlatform()).isNull();
        assertThat(repeated.getId()).isEqualTo(confirmed.getId());
        assertThat(rescanned.getLocalScanState()).contains("manual_overrides")
            .contains(subtitleId.toString()).contains(episodeId.toString())
            .contains("\"missing\":true").contains("aac").contains("重扫完成");
        verify(episodeService, times(1)).save(any());
        verify(attachmentReferenceService, times(1)).save(any());
        verify(taskService, times(3)).submit(any());
    }

    @Test
    void confirmShouldKeepUserEpisodeReferenceAsPending() {
        UUID directoryId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        DirectoryBindingWorkflowEntity workflow = DirectoryBindingWorkflowEntity.builder()
            .id(UUID.randomUUID()).directoryId(directoryId).subjectId(subjectId)
            .directoryName("local").localMode(LocalMediaMode.EPISODE.name()).build();
        AttachmentReference userReference = AttachmentReference.builder()
            .type(AttachmentReferenceType.EPISODE).attachmentId(videoId)
            .referenceId(UUID.randomUUID()).build();

        when(localMediaScanner.scan(any())).thenReturn(Mono.just(preview(directoryId, videoId, null)));
        when(workflowRepository.findLocalWorkflow(directoryId, subjectId, LocalMediaMode.EPISODE.name()))
            .thenReturn(Mono.just(workflow));
        when(workflowRepository.update(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(attachmentReferenceService.findAllByTypeAndAttachmentId(AttachmentReferenceType.EPISODE,
            videoId)).thenReturn(Flux.just(userReference));
        when(taskService.submit(any())).thenReturn(Mono.empty());

        DirectoryBindingWorkflowEntity result = service.confirm(LocalScanConfirmRequest.builder()
            .directoryId(directoryId).mode(LocalMediaMode.EPISODE).subjectId(subjectId).build()).block();

        assertThat(result.getLocalScanState()).contains(MediaRole.PENDING_CONFIRMATION.name());
        verify(episodeService, never()).save(any());
        verify(attachmentReferenceService, never()).save(any());
    }

    @Test
    void confirmShouldCreateSubjectAndSubmitQueryableTask() {
        UUID directoryId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        UUID episodeId = UUID.randomUUID();
        Subject requestedSubject = new Subject();
        Subject createdSubject = new Subject().setId(subjectId);
        AttachmentReference reference = AttachmentReference.builder()
            .type(AttachmentReferenceType.EPISODE).attachmentId(videoId).referenceId(episodeId).build();

        when(localMediaScanner.scan(any())).thenReturn(Mono.just(preview(directoryId, videoId, null)));
        when(subjectService.create(requestedSubject)).thenReturn(Mono.just(createdSubject));
        when(workflowRepository.findLocalWorkflow(directoryId, subjectId, LocalMediaMode.EPISODE.name()))
            .thenReturn(Mono.empty());
        when(workflowRepository.insert(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(workflowRepository.update(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(attachmentReferenceService.findAllByTypeAndAttachmentId(AttachmentReferenceType.EPISODE,
            videoId)).thenReturn(Flux.empty());
        when(episodeService.save(any()))
            .thenReturn(Mono.just(Episode.defaultEpisode(subjectId).setId(episodeId)));
        when(attachmentReferenceService.save(any())).thenReturn(Mono.just(reference));
        when(taskService.submit(any())).thenReturn(Mono.empty());

        DirectoryBindingWorkflowEntity workflow = service.confirm(LocalScanConfirmRequest.builder()
            .directoryId(directoryId).mode(LocalMediaMode.EPISODE).subject(requestedSubject).build()).block();

        assertThat(workflow.getSubjectId()).isEqualTo(subjectId);
        assertThat(workflow.getTaskId()).isNotNull();
        assertThat(workflow.getPlatform()).isNull();
        verify(subjectService).create(requestedSubject);
        verify(taskService).submit(any(LocalDirectoryBindingTask.class));
    }

    @Test
    void confirmShouldRejectInvalidSubjectSelectionAndEmptyPreview() {
        LocalScanConfirmRequest invalid = LocalScanConfirmRequest.builder()
            .directoryId(UUID.randomUUID()).mode(LocalMediaMode.EPISODE).build();
        LocalScanConfirmRequest duplicated = LocalScanConfirmRequest.builder()
            .directoryId(UUID.randomUUID()).mode(LocalMediaMode.EPISODE)
            .subjectId(UUID.randomUUID()).subject(new Subject()).build();

        assertThatThrownBy(() -> service.confirm(invalid).block())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("必须恰好提供一个条目");
        assertThatThrownBy(() -> service.confirm(duplicated).block())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("必须恰好提供一个条目");
        verify(localMediaScanner, never()).scan(any());

        UUID directoryId = UUID.randomUUID();
        when(localMediaScanner.scan(any())).thenReturn(Mono.just(LocalScanPreview.builder()
            .directoryId(directoryId).mode(LocalMediaMode.EPISODE).items(List.of()).build()));

        assertThatThrownBy(() -> service.confirm(LocalScanConfirmRequest.builder()
            .directoryId(directoryId).mode(LocalMediaMode.EPISODE).subjectId(UUID.randomUUID()).build()).block())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("没有可确认的主资源");
        verify(taskService, never()).submit(any());

        UUID videoId = UUID.randomUUID();
        UUID subtitleId = UUID.randomUUID();
        when(localMediaScanner.scan(any())).thenReturn(Mono.just(preview(directoryId, videoId, subtitleId)));
        assertThatThrownBy(() -> service.confirm(LocalScanConfirmRequest.builder()
            .directoryId(directoryId).mode(LocalMediaMode.EPISODE).subjectId(UUID.randomUUID())
            .assignments(List.of(LocalScanAssignment.builder().attachmentId(subtitleId)
                .primaryAttachmentId(UUID.randomUUID()).build())).build()).block())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("主资源不存在");
        verify(taskService, never()).submit(any());
    }

    @Test
    void confirmAudioShouldRequireMusicSubject() {
        UUID directoryId = UUID.randomUUID();
        UUID audioId = UUID.randomUUID();
        LocalScanPreview preview = LocalScanPreview.builder().directoryId(directoryId)
            .mode(LocalMediaMode.AUDIO)
            .items(List.of(LocalScanItem.builder().attachmentId(audioId)
                .relativePath("Track 1.flac").physicalType(MediaPhysicalType.AUDIO)
                .role(MediaRole.PRIMARY).build()))
            .build();
        when(localMediaScanner.scan(any())).thenReturn(Mono.just(preview));

        Subject anime = new Subject().setType(SubjectType.ANIME);
        assertThatThrownBy(() -> service.confirm(LocalScanConfirmRequest.builder()
            .directoryId(directoryId).mode(LocalMediaMode.AUDIO).subject(anime).build()).block())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("只能创建音乐条目");

        UUID subjectId = UUID.randomUUID();
        when(subjectService.findById(subjectId)).thenReturn(Mono.just(anime.setId(subjectId)));
        assertThatThrownBy(() -> service.confirm(LocalScanConfirmRequest.builder()
            .directoryId(directoryId).mode(LocalMediaMode.AUDIO).subjectId(subjectId).build()).block())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("只能绑定音乐条目");
        verify(subjectService, never()).create(any());
        verify(taskService, never()).submit(any());
    }

    private LocalScanPreview preview(UUID directoryId, UUID videoId, UUID subtitleId) {
        LocalScanItem primary = LocalScanItem.builder().attachmentId(videoId)
            .relativePath("Episode 1.mp4").physicalType(MediaPhysicalType.VIDEO)
            .role(MediaRole.PRIMARY)
            .tracks(List.of(MediaTrack.builder().index(0).kind("audio").codec("aac").build()))
            .build();
        if (subtitleId == null) {
            return LocalScanPreview.builder().directoryId(directoryId).mode(LocalMediaMode.EPISODE)
                .items(List.of(primary)).build();
        }
        LocalScanItem subtitle = LocalScanItem.builder().attachmentId(subtitleId)
            .relativePath("Episode 1.zh.srt").physicalType(MediaPhysicalType.SUBTITLE)
            .role(MediaRole.PENDING_CONFIRMATION).candidatePrimaryAttachmentId(videoId).build();
        return LocalScanPreview.builder().directoryId(directoryId).mode(LocalMediaMode.EPISODE)
            .items(List.of(primary, subtitle)).build();
    }
}
