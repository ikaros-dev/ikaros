package run.ikaros.media;

import static org.mockito.ArgumentMatchers.any;
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
}
