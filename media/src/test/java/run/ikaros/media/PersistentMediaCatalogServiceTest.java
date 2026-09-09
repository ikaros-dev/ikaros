package run.ikaros.media;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.resource.api.ResourceClassification;
import run.ikaros.resource.api.ResourceLifecycle;
import run.ikaros.resource.api.ResourceService;
import run.ikaros.resource.api.ResourceType;
import run.ikaros.resource.api.ResourceView;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PersistentMediaCatalogServiceTest {
    @Test
    void createsVideoSubjectWithOwnedResource() {
        UUID owner = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        ResourceService resources = org.mockito.Mockito.mock(ResourceService.class);
        MediaSubjectRepository subjects = org.mockito.Mockito.mock(MediaSubjectRepository.class);
        MediaSeasonRepository seasons = org.mockito.Mockito.mock(MediaSeasonRepository.class);
        MediaEpisodeRepository episodes = org.mockito.Mockito.mock(MediaEpisodeRepository.class);
        ResourceView resource = new ResourceView(resourceId, ResourceType.VIDEO, "示例视频", null,
            ResourceClassification.PRIVATE, ResourceLifecycle.ACTIVE, List.of(), List.of(), Instant.now(), Instant.now(), 0L);
        MediaSubjectEntity saved = new MediaSubjectEntity(subjectId, owner, resourceId, MediaSubjectKind.SERIES,
            Instant.now(), Instant.now(), 0L);
        when(resources.create(any(), any())).thenReturn(Mono.just(resource));
        when(subjects.save(any(MediaSubjectEntity.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(new PersistentMediaCatalogService(resources, subjects, seasons, episodes)
                .createSubject(owner, new CreateMediaSubjectRequest("示例视频", MediaSubjectKind.SERIES, "zh-CN")))
            .expectNextMatches(view -> view.id().equals(subjectId) && view.resourceId().equals(resourceId)
                && view.kind() == MediaSubjectKind.SERIES)
            .verifyComplete();
        verify(resources).create(org.mockito.ArgumentMatchers.eq(owner), any());
        verify(subjects).save(any(MediaSubjectEntity.class));
    }

    @Test
    void doesNotSaveSubjectWhenResourceCreationFails() {
        UUID owner = UUID.randomUUID();
        ResourceService resources = org.mockito.Mockito.mock(ResourceService.class);
        MediaSubjectRepository subjects = org.mockito.Mockito.mock(MediaSubjectRepository.class);
        MediaSeasonRepository seasons = org.mockito.Mockito.mock(MediaSeasonRepository.class);
        MediaEpisodeRepository episodes = org.mockito.Mockito.mock(MediaEpisodeRepository.class);
        when(resources.create(any(), any())).thenReturn(Mono.error(new IllegalStateException("resource unavailable")));

        StepVerifier.create(new PersistentMediaCatalogService(resources, subjects, seasons, episodes)
                .createSubject(owner, new CreateMediaSubjectRequest("示例视频", MediaSubjectKind.VIDEO, "zh-CN")))
            .expectErrorMatches(error -> error instanceof IllegalStateException
                && error.getMessage().equals("resource unavailable"))
            .verify();
        verify(subjects, never()).save(any(MediaSubjectEntity.class));
    }

    @Test
    void reordersAllEpisodesInOneSeason() {
        UUID owner = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID seasonId = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ResourceService resources = org.mockito.Mockito.mock(ResourceService.class);
        MediaSubjectRepository subjects = org.mockito.Mockito.mock(MediaSubjectRepository.class);
        MediaSeasonRepository seasons = org.mockito.Mockito.mock(MediaSeasonRepository.class);
        MediaEpisodeRepository episodes = org.mockito.Mockito.mock(MediaEpisodeRepository.class);
        when(subjects.findById(subjectId)).thenReturn(Mono.just(new MediaSubjectEntity(subjectId, owner, UUID.randomUUID(),
            MediaSubjectKind.SERIES, Instant.now(), Instant.now(), 0L)));
        when(seasons.findById(seasonId)).thenReturn(Mono.just(new MediaSeasonEntity(seasonId, owner, subjectId,
            UUID.randomUUID(), 1, "Season 1", Instant.now(), Instant.now(), 0L)));
        when(episodes.findAllByOwnerIdAndSeasonIdOrderByEpisodeNumberAsc(owner, seasonId)).thenReturn(reactor.core.publisher.Flux.just(
            new MediaEpisodeEntity(first, owner, subjectId, seasonId, UUID.randomUUID(), 0, null, null, Instant.now(), Instant.now(), 0L),
            new MediaEpisodeEntity(second, owner, subjectId, seasonId, UUID.randomUUID(), 1, null, null, Instant.now(), Instant.now(), 0L)));
        when(episodes.maxEpisodeNumber(owner, seasonId)).thenReturn(Mono.just(1));
        when(episodes.shiftEpisodeNumbers(owner, seasonId, 4)).thenReturn(Mono.just(2));
        when(episodes.updateEpisodeNumber(any(), any(), any(Integer.class))).thenReturn(Mono.just(1));

        StepVerifier.create(new PersistentMediaCatalogService(resources, subjects, seasons, episodes)
                .reorderEpisodes(owner, subjectId, seasonId, new ReorderMediaEpisodesRequest(List.of(second, first))))
            .verifyComplete();
        verify(episodes).shiftEpisodeNumbers(owner, seasonId, 4);
        verify(episodes).updateEpisodeNumber(eq(owner), eq(second), eq(0));
        verify(episodes).updateEpisodeNumber(eq(owner), eq(first), eq(1));
    }

    @Test
    void rejectsDuplicateEpisodeIdsBeforeWriting() {
        UUID owner = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID seasonId = UUID.randomUUID();
        UUID episodeId = UUID.randomUUID();
        MediaEpisodeRepository episodes = org.mockito.Mockito.mock(MediaEpisodeRepository.class);
        StepVerifier.create(new PersistentMediaCatalogService(org.mockito.Mockito.mock(ResourceService.class),
                org.mockito.Mockito.mock(MediaSubjectRepository.class), org.mockito.Mockito.mock(MediaSeasonRepository.class), episodes)
                .reorderEpisodes(owner, subjectId, seasonId, new ReorderMediaEpisodesRequest(List.of(episodeId, episodeId))))
            .expectErrorMessage("剧集顺序不能包含重复条目")
            .verify();
        verify(episodes, never()).shiftEpisodeNumbers(any(), any(), any(Integer.class));
    }
}
