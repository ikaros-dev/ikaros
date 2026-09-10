package run.ikaros.document;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.resource.api.ResourceLifecycle;
import run.ikaros.resource.api.ResourceService;
import run.ikaros.resource.api.ResourceType;
import run.ikaros.resource.api.ResourceView;

class PersistentDocumentServiceTest {
    @Test
    void createsResourceDocumentAndInitialWorkingCopy() {
        UUID owner = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        ResourceService resources = mock(ResourceService.class);
        DocumentRepository documents = mock(DocumentRepository.class);
        DocumentWorkingCopyRepository copies = mock(DocumentWorkingCopyRepository.class);
        DocumentRevisionRepository revisions = mock(DocumentRevisionRepository.class);
        DocumentPublicationRepository publications = mock(DocumentPublicationRepository.class);
        Instant now = Instant.now();
        when(resources.create(any(), any())).thenReturn(Mono.just(new ResourceView(resourceId, ResourceType.DOCUMENT, ResourceLifecycle.ACTIVE, List.of(), List.of(), now, now)));
        when(documents.save(any())).thenReturn(Mono.just(new DocumentEntity(documentId, owner, resourceId, DocumentKind.DOCUMENT, null, now, now, 0L)));
        when(copies.save(any())).thenReturn(Mono.just(mock(DocumentWorkingCopyEntity.class)));

        StepVerifier.create(new PersistentDocumentService(resources, documents, copies, revisions, publications)
                .create(owner, new CreateDocumentRequest("Draft", DocumentKind.DOCUMENT, "zh-CN", "初稿")))
            .expectNextMatches(view -> view.id().equals(documentId) && view.resourceId().equals(resourceId))
            .verifyComplete();
        verify(resources).create(any(), any());
        verify(copies).save(any());
    }
}
