package run.ikaros.server.core.episode;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.server.store.entity.EpisodeEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;

class EpisodeRemoveEventListenerTest {

    private AttachmentReferenceRepository attachmentReferenceRepository;
    private EpisodeRemoveEventListener listener;

    @BeforeEach
    void setUp() {
        attachmentReferenceRepository = mock(AttachmentReferenceRepository.class);
        listener = new EpisodeRemoveEventListener(attachmentReferenceRepository);
    }

    @Test
    void onEpisodeRemoveEvent_deletesReferences() {
        // Given
        UUID episodeId = UuidV7Utils.generateUuid();
        EpisodeEntity entity = EpisodeEntity.builder().name("ep-01").build();
        entity.setId(episodeId);
        EpisodeRemoveEvent event = new EpisodeRemoveEvent(this, entity);

        org.mockito.Mockito.when(attachmentReferenceRepository.deleteAllByTypeAndReferenceId(
                AttachmentReferenceType.EPISODE, episodeId))
            .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(listener.onEpisodeRemoveEvent(event))
            .verifyComplete();

        verify(attachmentReferenceRepository).deleteAllByTypeAndReferenceId(
            AttachmentReferenceType.EPISODE, episodeId);
    }
}
