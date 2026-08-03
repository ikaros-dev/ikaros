package run.ikaros.server.core.episode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.FetchSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.subject.EpisodeResource;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.entity.EpisodeEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.AttachmentRepository;
import run.ikaros.server.store.repository.EpisodeRepository;

class DefaultEpisodeServiceTest {
    private EpisodeRepository episodeRepository;
    private AttachmentReferenceRepository attachmentReferenceRepository;
    private AttachmentRepository attachmentRepository;
    private ApplicationEventPublisher applicationEventPublisher;
    private DatabaseClient databaseClient;
    private FetchSpec<Map<String, Object>> resourceFetchSpec;
    private Map<String, List<Map<String, Object>>> localStateRows;
    private DefaultEpisodeService defaultEpisodeService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        episodeRepository = Mockito.mock(EpisodeRepository.class);
        attachmentReferenceRepository =
            Mockito.mock(AttachmentReferenceRepository.class);
        attachmentRepository =
            Mockito.mock(AttachmentRepository.class);
        applicationEventPublisher =
            Mockito.mock(ApplicationEventPublisher.class);
        databaseClient = Mockito.mock(DatabaseClient.class);
        DatabaseClient.GenericExecuteSpec resourceSpec =
            Mockito.mock(DatabaseClient.GenericExecuteSpec.class);
        DatabaseClient.GenericExecuteSpec localStateSpec =
            Mockito.mock(DatabaseClient.GenericExecuteSpec.class);
        resourceFetchSpec = Mockito.mock(FetchSpec.class);
        FetchSpec<Map<String, Object>> localStateFetchSpec = Mockito.mock(FetchSpec.class);
        AtomicReference<String> localStateKey = new AtomicReference<>();
        localStateRows = new HashMap<>();
        when(databaseClient.sql(anyString())).thenAnswer(invocation ->
            invocation.<String>getArgument(0).contains("ATTACHMENT_REFERENCE")
                ? resourceSpec : localStateSpec);
        when(resourceSpec.bind(anyString(), any())).thenReturn(resourceSpec);
        when(resourceSpec.fetch()).thenReturn(resourceFetchSpec);
        when(localStateSpec.bind(anyString(), any())).thenAnswer(invocation -> {
            localStateKey.set(invocation.getArgument(1));
            return localStateSpec;
        });
        when(localStateSpec.fetch()).thenReturn(localStateFetchSpec);
        when(localStateFetchSpec.all()).thenAnswer(invocation -> Flux.fromIterable(
            localStateRows.getOrDefault(localStateKey.get(), List.of())));
        defaultEpisodeService = new DefaultEpisodeService(
            episodeRepository, attachmentReferenceRepository,
            attachmentRepository, applicationEventPublisher,
            databaseClient, new ObjectMapper());
    }

    @Test
    void save_newEpisode() {
        Episode episode = Episode.builder()
            .name("ep-01")
            .group(EpisodeGroup.MAIN)
            .sequence(1f)
            .subjectId(UuidV7Utils.generateUuid())
            .build();

        when(episodeRepository.insert(any(EpisodeEntity.class)))
            .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(defaultEpisodeService.save(episode))
            .assertNext(saved -> {
                assertThat(saved.getName()).isEqualTo("ep-01");
                assertThat(saved.getGroup())
                    .isEqualTo(EpisodeGroup.MAIN);
                assertThat(saved.getId()).isNotNull();
            })
            .verifyComplete();

        verify(episodeRepository)
            .insert(any(EpisodeEntity.class));
        verify(episodeRepository, never()).update(any());
    }

    @Test
    void save_existingEpisode() {
        UUID episodeId = UuidV7Utils.generateUuid();
        Episode episode = Episode.builder()
            .id(episodeId)
            .name("ep-01")
            .group(EpisodeGroup.MAIN)
            .sequence(1f)
            .subjectId(UuidV7Utils.generateUuid())
            .build();

        EpisodeEntity existingEntity = EpisodeEntity.builder()
            .name("ep-old")
            .group(EpisodeGroup.MAIN)
            .sequence(1f)
            .subjectId(episode.getSubjectId())
            .build();
        existingEntity.setId(episodeId);

        when(episodeRepository.findById(episodeId))
            .thenReturn(Mono.just(existingEntity));
        when(episodeRepository.update(any(EpisodeEntity.class)))
            .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(defaultEpisodeService.save(episode))
            .assertNext(saved -> {
                assertThat(saved.getId())
                    .isEqualTo(episodeId);
                assertThat(saved.getName())
                    .isEqualTo("ep-01");
            })
            .verifyComplete();

        verify(episodeRepository)
            .update(any(EpisodeEntity.class));
        verify(episodeRepository, never()).insert(any());
    }

    @Test
    void findById_found() {
        UUID episodeId = UuidV7Utils.generateUuid();
        EpisodeEntity entity = EpisodeEntity.builder()
            .name("ep-01")
            .group(EpisodeGroup.MAIN)
            .sequence(1f)
            .subjectId(UuidV7Utils.generateUuid())
            .build();
        entity.setId(episodeId);

        when(episodeRepository.findById(episodeId))
            .thenReturn(Mono.just(entity));

        StepVerifier.create(
                defaultEpisodeService.findById(episodeId))
            .assertNext(episode -> {
                assertThat(episode.getId())
                    .isEqualTo(episodeId);
                assertThat(episode.getName())
                    .isEqualTo("ep-01");
            })
            .verifyComplete();
    }

    @Test
    void findById_notFound() {
        UUID episodeId = UuidV7Utils.generateUuid();
        when(episodeRepository.findById(episodeId))
            .thenReturn(Mono.empty());

        StepVerifier.create(
                defaultEpisodeService.findById(episodeId))
            .verifyComplete();
    }

    @Test
    void findAllBySubjectId_multipleEpisodes() {
        UUID subjectId = UuidV7Utils.generateUuid();
        EpisodeEntity entity1 = EpisodeEntity.builder()
            .name("ep-01").group(EpisodeGroup.MAIN)
            .sequence(1f).subjectId(subjectId).build();
        entity1.setId(UuidV7Utils.generateUuid());
        EpisodeEntity entity2 = EpisodeEntity.builder()
            .name("ep-02").group(EpisodeGroup.MAIN)
            .sequence(2f).subjectId(subjectId).build();
        entity2.setId(UuidV7Utils.generateUuid());
        EpisodeEntity entity3 = EpisodeEntity.builder()
            .name("ep-03").group(EpisodeGroup.MAIN)
            .sequence(3f).subjectId(subjectId).build();
        entity3.setId(UuidV7Utils.generateUuid());

        when(episodeRepository.findAllBySubjectId(subjectId))
            .thenReturn(Flux.just(entity1, entity2, entity3));

        StepVerifier.create(
                defaultEpisodeService
                    .findAllBySubjectId(subjectId))
            .expectNextCount(3)
            .verifyComplete();
    }

    @Test
    void deleteById_success() {
        UUID episodeId = UuidV7Utils.generateUuid();
        EpisodeEntity entity = EpisodeEntity.builder()
            .name("ep-01").group(EpisodeGroup.MAIN)
            .sequence(1f)
            .subjectId(UuidV7Utils.generateUuid())
            .build();
        entity.setId(episodeId);

        when(episodeRepository.findById(episodeId))
            .thenReturn(Mono.just(entity));
        when(episodeRepository
            .delete(any(EpisodeEntity.class)))
            .thenReturn(Mono.empty());

        StepVerifier.create(
                defaultEpisodeService.deleteById(episodeId))
            .verifyComplete();

        verify(episodeRepository).delete(entity);

        ArgumentCaptor<EpisodeRemoveEvent> captor =
            ArgumentCaptor.forClass(
                EpisodeRemoveEvent.class);
        verify(applicationEventPublisher)
            .publishEvent(captor.capture());
        assertThat(captor.getValue().getEntity())
            .isEqualTo(entity);
    }

    @Test
    void countBySubjectId_success() {
        UUID subjectId = UuidV7Utils.generateUuid();
        when(episodeRepository.countBySubjectId(subjectId))
            .thenReturn(Mono.just(5L));

        StepVerifier.create(
                defaultEpisodeService
                    .countBySubjectId(subjectId))
            .expectNext(5L)
            .verifyComplete();
    }

    @Test
    void findResourcesById_withoutLocalStateReturnsOriginalResourceWithEmptyTracks() {
        UUID episodeId = UuidV7Utils.generateUuid();
        UUID attachmentId = UuidV7Utils.generateUuid();
        stubResourceRows(List.of(resourceRow(episodeId, attachmentId, "episode.mp4")));
        stubLocalStates(attachmentId, List.of());

        StepVerifier.create(defaultEpisodeService.findResourcesById(episodeId))
            .assertNext(resource -> {
                assertThat(resource.getAttachmentId()).isEqualTo(attachmentId);
                assertThat(resource.getTracks()).isEmpty();
                assertThat(resource.isImageSequence()).isFalse();
            })
            .verifyComplete();
    }

    @Test
    void findResourcesById_projectsEmbeddedAndExternalTracks() {
        UUID episodeId = UuidV7Utils.generateUuid();
        UUID videoId = UuidV7Utils.generateUuid();
        UUID subtitleId = UuidV7Utils.generateUuid();
        UUID audioId = UuidV7Utils.generateUuid();
        stubResourceRows(List.of(resourceRow(episodeId, videoId, "episode.mp4")));
        stubLocalStates(videoId, List.of(localVideoState(episodeId, videoId, subtitleId, audioId)));
        when(attachmentRepository.findById(subtitleId)).thenReturn(Mono.just(
            AttachmentEntity.builder().id(subtitleId).name("episode.zh-CN.srt").build()));
        when(attachmentRepository.findById(audioId)).thenReturn(Mono.just(
            AttachmentEntity.builder().id(audioId).name("episode.ja.m4a").build()));

        StepVerifier.create(defaultEpisodeService.findResourcesById(episodeId))
            .assertNext(resource -> {
                assertThat(resource.getTracks()).hasSize(3);
                assertThat(resource.getTracks().get(0))
                    .extracting("kind", "language", "playable", "attachmentId", "url")
                    .containsExactly("audio", "jpn", false, null, null);
                assertThat(resource.getTracks().subList(1, 3))
                    .extracting("kind", "attachmentId", "url", "playable")
                    .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("audio", audioId,
                            "/api/v1/attachment/stream/id/" + audioId, true),
                        org.assertj.core.groups.Tuple.tuple("subtitle", subtitleId,
                            "/api/v1/attachment/stream/id/" + subtitleId, true));
            })
            .verifyComplete();
    }

    @Test
    void findResourcesById_probeFailureAndMissingTrackDoNotFailResource() {
        UUID episodeId = UuidV7Utils.generateUuid();
        UUID videoId = UuidV7Utils.generateUuid();
        UUID missingSubtitleId = UuidV7Utils.generateUuid();
        stubResourceRows(List.of(resourceRow(episodeId, videoId, "broken.mp4")));
        String state = """
            {"items":[
              {"attachment_id":"%s","episode_id":"%s","physical_type":"VIDEO",
               "role":"PRIMARY","tracks":[],"probe_failure_reason":"容器已损坏"},
              {"attachment_id":"%s","physical_type":"SUBTITLE","role":"AUTO_ASSOCIATED",
               "candidate_primary_attachment_id":"%s","relative_path":"broken.srt"}
            ]}
            """.formatted(videoId, episodeId, missingSubtitleId, videoId);
        stubLocalStates(videoId, List.of("not-json", state));
        when(attachmentRepository.findById(missingSubtitleId)).thenReturn(Mono.empty());

        StepVerifier.create(defaultEpisodeService.findResourcesById(episodeId))
            .assertNext(resource -> {
                assertThat(resource.getName()).isEqualTo("broken.mp4");
                assertThat(resource.getTracks()).singleElement()
                    .satisfies(track -> {
                        assertThat(track.isPlayable()).isFalse();
                        assertThat(track.getFailureReason()).isEqualTo("容器已损坏");
                    });
            })
            .verifyComplete();
    }

    @Test
    void findResourcesById_imageResourcesUseNaturalOrder() {
        UUID episodeId = UuidV7Utils.generateUuid();
        UUID image10Id = UuidV7Utils.generateUuid();
        UUID image2Id = UuidV7Utils.generateUuid();
        stubResourceRows(List.of(
            resourceRow(episodeId, image10Id, "page10.jpg"),
            resourceRow(episodeId, image2Id, "page2.jpg")));
        stubLocalStates(image10Id, List.of(localImageState(episodeId, image10Id,
            "chapter2/page10.jpg")));
        stubLocalStates(image2Id, List.of(localImageState(episodeId, image2Id,
            "chapter10/page2.jpg")));

        StepVerifier.create(defaultEpisodeService.findResourcesById(episodeId).collectList())
            .assertNext(resources -> {
                assertThat(resources).extracting(EpisodeResource::getName)
                    .containsExactly("page10.jpg", "page2.jpg");
                assertThat(resources).allMatch(EpisodeResource::isImageSequence);
                assertThat(resources).allSatisfy(resource ->
                    assertThat(resource.getTracks()).isEmpty());
            })
            .verifyComplete();
    }

    private void stubResourceRows(List<Map<String, Object>> rows) {
        when(resourceFetchSpec.all()).thenReturn(Flux.fromIterable(rows));
    }

    private void stubLocalStates(UUID attachmentId, List<String> states) {
        List<Map<String, Object>> rows = states.stream()
            .map(state -> Map.<String, Object>of("local_scan_state", state))
            .toList();
        localStateRows.put('%' + attachmentId.toString() + '%', rows);
    }

    private Map<String, Object> resourceRow(UUID episodeId, UUID attachmentId, String name) {
        return Map.of(
            "attachment_id", attachmentId,
            "episode_id", episodeId,
            "url", "/api/v1/attachment/stream/id/" + attachmentId,
            "name", name);
    }

    private String localVideoState(UUID episodeId, UUID videoId, UUID subtitleId, UUID audioId) {
        return """
            {"items":[
              {"attachment_id":"%s","episode_id":"%s","physical_type":"VIDEO",
               "role":"PRIMARY","tracks":[{"index":1,"kind":"audio","language":"jpn",
               "default_track":true,"codec":"aac","playable":true}]},
              {"attachment_id":"%s","physical_type":"SUBTITLE","role":"AUTO_ASSOCIATED",
               "candidate_primary_attachment_id":"%s","relative_path":"episode.zh-CN.srt",
               "display_metadata":{"extension":".srt"}},
              {"attachment_id":"%s","physical_type":"AUDIO","role":"AUTO_ASSOCIATED",
               "candidate_primary_attachment_id":"%s","relative_path":"episode.ja.m4a",
               "display_metadata":{"extension":".m4a"}}
            ]}
            """.formatted(videoId, episodeId, subtitleId, videoId, audioId, videoId);
    }

    private String localImageState(UUID episodeId, UUID imageId, String relativePath) {
        return """
            {"items":[{"attachment_id":"%s","episode_id":"%s",
              "relative_path":"%s","physical_type":"IMAGE","role":"PRIMARY","tracks":[]}]}
            """.formatted(imageId, episodeId, relativePath);
    }
}
