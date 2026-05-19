package run.ikaros.server.core.episode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.EpisodeResource;
import run.ikaros.api.store.enums.EpisodeGroup;

class DefaultEpisodeOperateTest {

    private EpisodeService service;
    private DefaultEpisodeOperate operate;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(EpisodeService.class);
        operate = new DefaultEpisodeOperate(service);
    }

    @Test
    void constructorCreatesInstance() {
        assertThat(operate).isNotNull();
    }

    @Test
    void save_delegatesToService() {
        Episode episode = Episode.builder()
            .name("ep-01")
            .group(EpisodeGroup.MAIN)
            .sequence(1f)
            .subjectId(UUID.randomUUID())
            .build();
        Episode saved = Episode.builder()
            .id(UUID.randomUUID())
            .name("ep-01")
            .group(EpisodeGroup.MAIN)
            .sequence(1f)
            .subjectId(episode.getSubjectId())
            .build();

        when(service.save(episode)).thenReturn(Mono.just(saved));

        StepVerifier.create(operate.save(episode))
            .assertNext(result -> {
                assertThat(result).isSameAs(saved);
                assertThat(result.getName()).isEqualTo("ep-01");
            })
            .verifyComplete();

        verify(service).save(episode);
    }

    @Test
    void findById_delegatesToService() {
        UUID episodeId = UUID.randomUUID();
        Episode episode = Episode.builder()
            .id(episodeId)
            .name("ep-01")
            .group(EpisodeGroup.MAIN)
            .sequence(1f)
            .subjectId(UUID.randomUUID())
            .build();

        when(service.findById(episodeId)).thenReturn(Mono.just(episode));

        StepVerifier.create(operate.findById(episodeId))
            .assertNext(result -> assertThat(result).isSameAs(episode))
            .verifyComplete();

        verify(service).findById(episodeId);
    }

    @Test
    void findAllBySubjectId_delegatesToService() {
        UUID subjectId = UUID.randomUUID();
        Episode ep1 = Episode.builder()
            .id(UUID.randomUUID())
            .name("ep-01")
            .group(EpisodeGroup.MAIN)
            .sequence(1f)
            .subjectId(subjectId)
            .build();
        Episode ep2 = Episode.builder()
            .id(UUID.randomUUID())
            .name("ep-02")
            .group(EpisodeGroup.MAIN)
            .sequence(2f)
            .subjectId(subjectId)
            .build();

        when(service.findAllBySubjectId(subjectId))
            .thenReturn(Flux.just(ep1, ep2));

        StepVerifier.create(operate.findAllBySubjectId(subjectId))
            .expectNext(ep1, ep2)
            .verifyComplete();

        verify(service).findAllBySubjectId(subjectId);
    }

    @Test
    void countBySubjectId_delegatesToService() {
        UUID subjectId = UUID.randomUUID();
        when(service.countBySubjectId(subjectId))
            .thenReturn(Mono.just(10L));

        StepVerifier.create(operate.countBySubjectId(subjectId))
            .assertNext(count -> assertThat(count).isEqualTo(10L))
            .verifyComplete();

        verify(service).countBySubjectId(subjectId);
    }

    @Test
    void findBySubjectIdAndGroupAndSequenceAndName_delegatesToService() {
        UUID subjectId = UUID.randomUUID();
        Episode episode = Episode.builder()
            .id(UUID.randomUUID())
            .name("ep-01")
            .group(EpisodeGroup.MAIN)
            .sequence(1f)
            .subjectId(subjectId)
            .build();

        when(service.findBySubjectIdAndGroupAndSequenceAndName(
            subjectId, EpisodeGroup.MAIN, 1f, "ep-01"))
            .thenReturn(Mono.just(episode));

        StepVerifier.create(operate.findBySubjectIdAndGroupAndSequenceAndName(
                subjectId, EpisodeGroup.MAIN, 1f, "ep-01"))
            .assertNext(result -> assertThat(result).isSameAs(episode))
            .verifyComplete();

        verify(service).findBySubjectIdAndGroupAndSequenceAndName(
            subjectId, EpisodeGroup.MAIN, 1f, "ep-01");
    }

    @Test
    void findBySubjectIdAndGroupAndSequence_delegatesToService() {
        UUID subjectId = UUID.randomUUID();
        Episode ep1 = Episode.builder()
            .id(UUID.randomUUID())
            .name("ep-01")
            .group(EpisodeGroup.MAIN)
            .sequence(1f)
            .subjectId(subjectId)
            .build();

        when(service.findBySubjectIdAndGroupAndSequence(subjectId, EpisodeGroup.MAIN, 1f))
            .thenReturn(Flux.just(ep1));

        StepVerifier.create(operate.findBySubjectIdAndGroupAndSequence(subjectId, EpisodeGroup.MAIN, 1f))
            .assertNext(result -> assertThat(result).isSameAs(ep1))
            .verifyComplete();

        verify(service).findBySubjectIdAndGroupAndSequence(subjectId, EpisodeGroup.MAIN, 1f);
    }

    @Test
    void countMatchingBySubjectId_delegatesToService() {
        UUID subjectId = UUID.randomUUID();
        when(service.countMatchingBySubjectId(subjectId))
            .thenReturn(Mono.just(5L));

        StepVerifier.create(operate.countMatchingBySubjectId(subjectId))
            .assertNext(count -> assertThat(count).isEqualTo(5L))
            .verifyComplete();

        verify(service).countMatchingBySubjectId(subjectId);
    }

    @Test
    void findResourcesById_delegatesToService() {
        UUID episodeId = UUID.randomUUID();
        EpisodeResource resource = new EpisodeResource();
        resource.setEpisodeId(episodeId);

        when(service.findResourcesById(episodeId))
            .thenReturn(Flux.just(resource));

        StepVerifier.create(operate.findResourcesById(episodeId))
            .assertNext(result -> assertThat(result).isSameAs(resource))
            .verifyComplete();

        verify(service).findResourcesById(episodeId);
    }

    @Test
    void updateEpisodesWithSubjectId_delegatesToService() {
        UUID subjectId = UUID.randomUUID();
        Episode ep = Episode.builder()
            .id(UUID.randomUUID())
            .name("ep-01")
            .group(EpisodeGroup.MAIN)
            .sequence(1f)
            .subjectId(subjectId)
            .build();

        when(service.updateEpisodesWithSubjectId(subjectId, List.of(ep)))
            .thenReturn(Flux.just(ep));

        StepVerifier.create(operate.updateEpisodesWithSubjectId(subjectId, List.of(ep)))
            .assertNext(result -> assertThat(result).isSameAs(ep))
            .verifyComplete();

        verify(service).updateEpisodesWithSubjectId(subjectId, List.of(ep));
    }
}
