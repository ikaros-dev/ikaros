package run.ikaros.server.core.episode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.EpisodeGroup;
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
    private DefaultEpisodeService defaultEpisodeService;

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
        defaultEpisodeService = new DefaultEpisodeService(
            episodeRepository, attachmentReferenceRepository,
            attachmentRepository, applicationEventPublisher,
            databaseClient);
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
}
