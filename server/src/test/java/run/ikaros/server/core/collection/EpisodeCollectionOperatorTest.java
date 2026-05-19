package run.ikaros.server.core.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.collection.EpisodeCollection;

class EpisodeCollectionOperatorTest {

    @Mock
    private EpisodeCollectionService service;

    private EpisodeCollectionOperator operator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        operator = new EpisodeCollectionOperator(service);
    }

    @Test
    void constructorCreatesInstance() {
        assertThat(operator).isNotNull();
    }

    @Test
    void createDelegatesToService() {
        UUID userId = UUID.randomUUID();
        UUID episodeId = UUID.randomUUID();
        EpisodeCollection expected = EpisodeCollection.builder()
            .userId(userId)
            .episodeId(episodeId)
            .build();

        when(service.create(userId, episodeId)).thenReturn(Mono.just(expected));

        StepVerifier.create(operator.create(userId, episodeId))
            .assertNext(result -> {
                assertThat(result).isSameAs(expected);
                assertThat(result.getUserId()).isEqualTo(userId);
                assertThat(result.getEpisodeId()).isEqualTo(episodeId);
            })
            .verifyComplete();

        verify(service).create(userId, episodeId);
    }

    @Test
    void findByUserIdAndEpisodeIdDelegatesToService() {
        UUID userId = UUID.randomUUID();
        UUID episodeId = UUID.randomUUID();
        EpisodeCollection expected = EpisodeCollection.builder()
            .userId(userId)
            .episodeId(episodeId)
            .build();

        when(service.findByUserIdAndEpisodeId(userId, episodeId))
            .thenReturn(Mono.just(expected));

        StepVerifier.create(operator.findByUserIdAndEpisodeId(userId, episodeId))
            .assertNext(result -> assertThat(result).isSameAs(expected))
            .verifyComplete();

        verify(service).findByUserIdAndEpisodeId(userId, episodeId);
    }

    @Test
    void updateEpisodeCollectionProgressDelegatesToService() {
        UUID userId = UUID.randomUUID();
        UUID episodeId = UUID.randomUUID();
        Long progress = 120000L;

        when(service.updateEpisodeCollectionProgress(userId, episodeId, progress))
            .thenReturn(Mono.empty());

        StepVerifier.create(operator.updateEpisodeCollectionProgress(userId, episodeId, progress))
            .verifyComplete();

        verify(service).updateEpisodeCollectionProgress(userId, episodeId, progress);
    }

    @Test
    void updateEpisodeCollectionDelegatesToService() {
        UUID userId = UUID.randomUUID();
        UUID episodeId = UUID.randomUUID();
        Long progress = 120000L;
        Long duration = 300000L;

        when(service.updateEpisodeCollection(userId, episodeId, progress, duration))
            .thenReturn(Mono.empty());

        StepVerifier.create(operator.updateEpisodeCollection(userId, episodeId, progress, duration))
            .verifyComplete();

        verify(service).updateEpisodeCollection(userId, episodeId, progress, duration);
    }

    @Test
    void updateEpisodeCollectionFinishDelegatesToService() {
        UUID userId = UUID.randomUUID();
        UUID episodeId = UUID.randomUUID();

        when(service.updateEpisodeCollectionFinish(userId, episodeId, true))
            .thenReturn(Mono.empty());

        StepVerifier.create(operator.updateEpisodeCollectionFinish(userId, episodeId, true))
            .verifyComplete();

        verify(service).updateEpisodeCollectionFinish(userId, episodeId, true);
    }
}
