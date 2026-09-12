package run.ikaros.document;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PersistentDocumentEmbedServiceTest {
    @Test
    void createsEmbedForOwnedDocument() {
        UUID owner = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        DocumentRepository documents = mock(DocumentRepository.class);
        DocumentEmbedRepository embeds = mock(DocumentEmbedRepository.class);
        Instant now = Instant.now();
        when(documents.findById(documentId)).thenReturn(Mono.just(new DocumentEntity(documentId, owner, UUID.randomUUID(), DocumentKind.DOCUMENT, null, now, now, 0L)));
        when(embeds.save(any())).thenReturn(Mono.just(new DocumentEmbedEntity(UUID.randomUUID(), documentId, attachmentId, "图", "说明", "替代文本", now)));

        StepVerifier.create(new PersistentDocumentEmbedService(documents, embeds)
                .create(owner, documentId, new CreateDocumentEmbedRequest(attachmentId, "图", "说明", "替代文本")))
            .expectNextMatches(view -> view.documentId().equals(documentId) && view.attachmentId().equals(attachmentId))
            .verifyComplete();
        verify(embeds).save(any());
    }
}
