package run.ikaros.server.core.episode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.server.core.attachment.event.EpisodeAttachmentUpdateEvent;
import run.ikaros.server.core.notify.NotifyService;
import run.ikaros.server.store.repository.AttachmentRepository;
import run.ikaros.server.store.repository.EpisodeRepository;
import run.ikaros.server.store.repository.SubjectRepository;

class EpisodeAttachmentUpdateEventListenerTest {

    private AttachmentRepository attachmentRepository;
    private EpisodeRepository episodeRepository;
    private SubjectRepository subjectRepository;
    private NotifyService notifyService;
    private IkarosProperties ikarosProperties;
    private EpisodeAttachmentUpdateEventListener listener;

    @BeforeEach
    void setUp() {
        attachmentRepository = mock(AttachmentRepository.class);
        episodeRepository = mock(EpisodeRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        notifyService = mock(NotifyService.class);
        ikarosProperties = mock(IkarosProperties.class);
        listener = new EpisodeAttachmentUpdateEventListener(
            attachmentRepository, episodeRepository,
            subjectRepository, notifyService, ikarosProperties);
    }

    @Test
    void constructorCreatesInstance() {
        assertThat(listener).isNotNull();
    }

    @Test
    void onEpisodeAttachmentUpdateEvent_notifyFalse_returnsEmpty() {
        UUID episodeId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        EpisodeAttachmentUpdateEvent event =
            new EpisodeAttachmentUpdateEvent(this, episodeId,
                attachmentId, false);

        StepVerifier.create(
                listener.onEpisodeAttachmentUpdateEvent(event))
            .verifyComplete();

        verify(attachmentRepository, never()).findById(attachmentId);
        verify(episodeRepository, never()).findById(episodeId);
        verify(notifyService, never()).send(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any());
    }
}
