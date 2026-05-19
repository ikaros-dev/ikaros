package run.ikaros.server.core.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.server.store.entity.EpisodeCollectionEntity;
import run.ikaros.server.store.entity.EpisodeEntity;
import run.ikaros.server.store.repository.EpisodeCollectionRepository;
import run.ikaros.server.store.repository.EpisodeRepository;

class EpisodeCollectionServiceImplTest {

    @Mock
    private EpisodeCollectionRepository episodeCollectionRepository;
    @Mock
    private EpisodeRepository episodeRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private EpisodeCollectionServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new EpisodeCollectionServiceImpl(
            episodeCollectionRepository, episodeRepository, applicationEventPublisher);
    }

    @Test
    void constructorCreatesInstance() {
        assertThat(service).isNotNull();
    }

    @Test
    void create() {
        UUID userId = UUID.randomUUID();
        UUID episodeId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();

        EpisodeEntity episodeEntity = EpisodeEntity.builder()
            .subjectId(subjectId)
            .name("ep-1")
            .build();
        episodeEntity.setId(episodeId);

        EpisodeCollectionEntity savedEntity = EpisodeCollectionEntity.builder()
            .userId(userId)
            .subjectId(subjectId)
            .episodeId(episodeId)
            .finish(false)
            .build();
        savedEntity.setId(UUID.randomUUID());

        when(episodeCollectionRepository.findByUserIdAndEpisodeId(userId, episodeId))
            .thenReturn(Mono.empty());
        when(episodeRepository.findById(episodeId))
            .thenReturn(Mono.just(episodeEntity));
        when(episodeCollectionRepository.insert(any(EpisodeCollectionEntity.class)))
            .thenReturn(Mono.just(savedEntity));

        StepVerifier.create(service.create(userId, episodeId))
            .assertNext(result -> {
                assertThat(result).isNotNull();
                assertThat(result.getUserId()).isEqualTo(userId);
                assertThat(result.getEpisodeId()).isEqualTo(episodeId);
            })
            .verifyComplete();

        verify(episodeCollectionRepository).insert(any(EpisodeCollectionEntity.class));
    }

    @Test
    void createWhenAlreadyExists() {
        UUID userId = UUID.randomUUID();
        UUID episodeId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();

        EpisodeCollectionEntity existingEntity = EpisodeCollectionEntity.builder()
            .userId(userId)
            .subjectId(subjectId)
            .episodeId(episodeId)
            .finish(false)
            .build();
        existingEntity.setId(UUID.randomUUID());

        EpisodeEntity episodeEntity = EpisodeEntity.builder()
            .subjectId(subjectId)
            .name("ep-1")
            .build();
        episodeEntity.setId(episodeId);

        when(episodeCollectionRepository.findByUserIdAndEpisodeId(userId, episodeId))
            .thenReturn(Mono.just(existingEntity));
        when(episodeRepository.findById(episodeId))
            .thenReturn(Mono.just(episodeEntity));

        StepVerifier.create(service.create(userId, episodeId))
            .assertNext(result -> {
                assertThat(result).isNotNull();
                assertThat(result.getUserId()).isEqualTo(userId);
                assertThat(result.getEpisodeId()).isEqualTo(episodeId);
            })
            .verifyComplete();

        verify(episodeCollectionRepository, never()).insert(any(EpisodeCollectionEntity.class));
    }

    @Test
    void findByUserIdAndEpisodeId() {
        UUID userId = UUID.randomUUID();
        UUID episodeId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();

        EpisodeCollectionEntity entity = EpisodeCollectionEntity.builder()
            .userId(userId)
            .subjectId(subjectId)
            .episodeId(episodeId)
            .finish(false)
            .build();
        entity.setId(UUID.randomUUID());

        EpisodeEntity episodeEntity = EpisodeEntity.builder()
            .subjectId(subjectId)
            .name("ep-1")
            .build();
        episodeEntity.setId(episodeId);

        when(episodeCollectionRepository.findByUserIdAndEpisodeId(userId, episodeId))
            .thenReturn(Mono.just(entity));
        when(episodeRepository.findById(episodeId))
            .thenReturn(Mono.just(episodeEntity));

        StepVerifier.create(service.findByUserIdAndEpisodeId(userId, episodeId))
            .assertNext(result -> {
                assertThat(result).isNotNull();
                assertThat(result.getUserId()).isEqualTo(userId);
                assertThat(result.getEpisodeId()).isEqualTo(episodeId);
            })
            .verifyComplete();
    }

    @Test
    void findAllByUserIdAndSubjectId() {
        UUID userId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID episodeId = UUID.randomUUID();

        EpisodeCollectionEntity entity = EpisodeCollectionEntity.builder()
            .userId(userId)
            .subjectId(subjectId)
            .episodeId(episodeId)
            .finish(false)
            .build();
        entity.setId(UUID.randomUUID());

        EpisodeEntity episodeEntity = EpisodeEntity.builder()
            .subjectId(subjectId)
            .name("ep-1")
            .build();
        episodeEntity.setId(episodeId);

        when(episodeCollectionRepository.findAllByUserIdAndSubjectId(userId, subjectId))
            .thenReturn(Flux.just(entity));
        when(episodeRepository.findById(episodeId))
            .thenReturn(Mono.just(episodeEntity));

        StepVerifier.create(service.findAllByUserIdAndSubjectId(userId, subjectId))
            .assertNext(result -> {
                assertThat(result).isNotNull();
                assertThat(result.getUserId()).isEqualTo(userId);
                assertThat(result.getSubjectId()).isEqualTo(subjectId);
            })
            .verifyComplete();
    }

    @Test
    void updateEpisodeCollectionProgress() {
        UUID userId = UUID.randomUUID();
        UUID episodeId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();

        EpisodeCollectionEntity entity = EpisodeCollectionEntity.builder()
            .userId(userId)
            .subjectId(subjectId)
            .episodeId(episodeId)
            .finish(false)
            .build();
        entity.setId(UUID.randomUUID());

        EpisodeEntity episodeEntity = EpisodeEntity.builder()
            .subjectId(subjectId)
            .name("ep-1")
            .build();
        episodeEntity.setId(episodeId);

        when(episodeCollectionRepository.findByUserIdAndEpisodeId(userId, episodeId))
            .thenReturn(Mono.just(entity));
        when(episodeRepository.findById(episodeId))
            .thenReturn(Mono.just(episodeEntity));
        when(episodeCollectionRepository.save(any(EpisodeCollectionEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.updateEpisodeCollectionProgress(userId, episodeId, 100L))
            .verifyComplete();

        verify(episodeCollectionRepository).save(any(EpisodeCollectionEntity.class));
    }

    @Test
    void updateEpisodeCollectionFinish() {
        UUID userId = UUID.randomUUID();
        UUID episodeId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();

        EpisodeCollectionEntity entity = EpisodeCollectionEntity.builder()
            .userId(userId)
            .subjectId(subjectId)
            .episodeId(episodeId)
            .finish(false)
            .build();
        entity.setId(UUID.randomUUID());

        when(episodeCollectionRepository.findByUserIdAndEpisodeId(userId, episodeId))
            .thenReturn(Mono.just(entity));
        when(episodeCollectionRepository.save(any(EpisodeCollectionEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.updateEpisodeCollectionFinish(userId, episodeId, true))
            .verifyComplete();

        verify(episodeCollectionRepository).save(any(EpisodeCollectionEntity.class));
        verify(applicationEventPublisher).publishEvent(any());
    }

    @Test
    void remove() {
        UUID userId = UUID.randomUUID();
        UUID episodeId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();

        EpisodeCollectionEntity entity = EpisodeCollectionEntity.builder()
            .userId(userId)
            .subjectId(subjectId)
            .episodeId(episodeId)
            .finish(false)
            .build();
        entity.setId(UUID.randomUUID());

        EpisodeEntity episodeEntity = EpisodeEntity.builder()
            .subjectId(subjectId)
            .name("ep-1")
            .build();
        episodeEntity.setId(episodeId);

        when(episodeCollectionRepository.findByUserIdAndEpisodeId(userId, episodeId))
            .thenReturn(Mono.just(entity));
        when(episodeCollectionRepository.delete(any(EpisodeCollectionEntity.class)))
            .thenReturn(Mono.empty());
        when(episodeRepository.findById(episodeId))
            .thenReturn(Mono.just(episodeEntity));

        StepVerifier.create(service.remove(userId, episodeId))
            .assertNext(result -> {
                assertThat(result).isNotNull();
                assertThat(result.getUserId()).isEqualTo(userId);
                assertThat(result.getEpisodeId()).isEqualTo(episodeId);
            })
            .verifyComplete();

        verify(episodeCollectionRepository).delete(any(EpisodeCollectionEntity.class));
    }
}
