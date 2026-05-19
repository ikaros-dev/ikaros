package run.ikaros.server.core.attachment.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.server.core.attachment.event.AttachmentRemoveEvent;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.AttachmentRelationRepository;

class AttachmentRemoveListenerTest {

    @Mock
    private AttachmentRelationRepository relationRepository;
    @Mock
    private AttachmentReferenceRepository referenceRepository;
    private AttachmentRemoveListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new AttachmentRemoveListener(relationRepository, referenceRepository);
    }

    @Test
    void onAttachmentRemoveEvent_deletesRelationsAndReferences() {
        UUID attId = UUID.randomUUID();
        AttachmentEntity entity = AttachmentEntity.builder().id(attId).build();
        AttachmentRemoveEvent event = new AttachmentRemoveEvent(this, entity);

        when(relationRepository.deleteAllByAttachmentId(attId)).thenReturn(Mono.empty());
        when(referenceRepository.deleteAllByAttachmentId(attId)).thenReturn(Mono.empty());

        StepVerifier.create(listener.onAttachmentRemoveEvent(event))
            .verifyComplete();

        verify(relationRepository).deleteAllByAttachmentId(attId);
        verify(referenceRepository).deleteAllByAttachmentId(attId);
    }
}
