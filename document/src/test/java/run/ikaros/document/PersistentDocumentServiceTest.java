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
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import run.ikaros.common.ConflictException;
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

    @Test
    void savesWorkingCopyWithMatchingVersion() {
        UUID owner = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceService resources = mock(ResourceService.class);
        DocumentRepository documents = mock(DocumentRepository.class);
        DocumentWorkingCopyRepository copies = mock(DocumentWorkingCopyRepository.class);
        DocumentRevisionRepository revisions = mock(DocumentRevisionRepository.class);
        DocumentPublicationRepository publications = mock(DocumentPublicationRepository.class);
        when(documents.findById(documentId)).thenReturn(Mono.just(new DocumentEntity(documentId, owner, UUID.randomUUID(), DocumentKind.DOCUMENT, null, now, now, 0L)));
        when(copies.findByDocumentId(documentId)).thenReturn(Mono.just(new DocumentWorkingCopyEntity(UUID.randomUUID(), documentId, owner, "old", "v1", null, now, 2L)));
        when(copies.save(any())).thenReturn(Mono.just(new DocumentWorkingCopyEntity(UUID.randomUUID(), documentId, owner, "new", "v1", null, now, 3L)));

        StepVerifier.create(new PersistentDocumentService(resources, documents, copies, revisions, publications)
                .updateWorkingCopy(owner, documentId, new UpdateWorkingCopyRequest("new", "v1", 2L)))
            .expectNextMatches(copy -> copy.content().equals("new"))
            .verifyComplete();
    }

    @Test
    void rejectsWorkingCopyWhenVersionChanged() {
        UUID owner = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceService resources = mock(ResourceService.class);
        DocumentRepository documents = mock(DocumentRepository.class);
        DocumentWorkingCopyRepository copies = mock(DocumentWorkingCopyRepository.class);
        DocumentRevisionRepository revisions = mock(DocumentRevisionRepository.class);
        DocumentPublicationRepository publications = mock(DocumentPublicationRepository.class);
        when(documents.findById(documentId)).thenReturn(Mono.just(new DocumentEntity(documentId, owner, UUID.randomUUID(), DocumentKind.DOCUMENT, null, now, now, 0L)));
        when(copies.findByDocumentId(documentId)).thenReturn(Mono.just(new DocumentWorkingCopyEntity(UUID.randomUUID(), documentId, owner, "server", "v1", null, now, 3L)));

        StepVerifier.create(new PersistentDocumentService(resources, documents, copies, revisions, publications)
                .updateWorkingCopy(owner, documentId, new UpdateWorkingCopyRequest("local", "v1", 2L)))
            .expectErrorMatches(error -> error instanceof ConflictException)
            .verify();
    }

    @Test
    void listsOnlyOwnedDocumentRevisionsInRepositoryOrder() {
        UUID owner = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceService resources = mock(ResourceService.class);
        DocumentRepository documents = mock(DocumentRepository.class);
        DocumentWorkingCopyRepository copies = mock(DocumentWorkingCopyRepository.class);
        DocumentRevisionRepository revisions = mock(DocumentRevisionRepository.class);
        DocumentPublicationRepository publications = mock(DocumentPublicationRepository.class);
        when(documents.findById(documentId)).thenReturn(Mono.just(new DocumentEntity(documentId, owner, UUID.randomUUID(), DocumentKind.DOCUMENT, null, now, now, 0L)));
        when(revisions.findAllByDocumentIdOrderByRevisionNumberDesc(documentId)).thenReturn(Flux.just(
                new DocumentRevisionEntity(UUID.randomUUID(), documentId, owner, 2L, "second", "v1", now, owner),
                new DocumentRevisionEntity(UUID.randomUUID(), documentId, owner, 1L, "first", "v1", now, owner)));

        StepVerifier.create(new PersistentDocumentService(resources, documents, copies, revisions, publications)
                .revisions(owner, documentId))
            .expectNextMatches(revision -> revision.revisionNumber() == 2L)
            .expectNextMatches(revision -> revision.revisionNumber() == 1L)
            .verifyComplete();
    }

    @Test
    void comparesTwoOwnedRevisionsByLine() {
        UUID owner = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceService resources = mock(ResourceService.class);
        DocumentRepository documents = mock(DocumentRepository.class);
        DocumentWorkingCopyRepository copies = mock(DocumentWorkingCopyRepository.class);
        DocumentRevisionRepository revisions = mock(DocumentRevisionRepository.class);
        DocumentPublicationRepository publications = mock(DocumentPublicationRepository.class);
        when(documents.findById(documentId)).thenReturn(Mono.just(new DocumentEntity(documentId, owner, UUID.randomUUID(), DocumentKind.DOCUMENT, null, now, now, 0L)));
        when(revisions.findAllByDocumentIdOrderByRevisionNumberDesc(documentId)).thenReturn(Flux.just(
                new DocumentRevisionEntity(UUID.randomUUID(), documentId, owner, 2L, "标题\n新内容", "v1", now, owner),
                new DocumentRevisionEntity(UUID.randomUUID(), documentId, owner, 1L, "标题\n旧内容", "v1", now, owner)));

        StepVerifier.create(new PersistentDocumentService(resources, documents, copies, revisions, publications)
                .compareRevisions(owner, documentId, 1L, 2L))
            .expectNextMatches(result -> result.lines().stream().anyMatch(line -> line.type().equals("REMOVED") && line.content().equals("旧内容"))
                    && result.lines().stream().anyMatch(line -> line.type().equals("ADDED") && line.content().equals("新内容")))
            .verifyComplete();
    }

    @Test
    void restoresRevisionIntoWorkingCopyAndCurrentDocument() {
        UUID owner = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceService resources = mock(ResourceService.class);
        DocumentRepository documents = mock(DocumentRepository.class);
        DocumentWorkingCopyRepository copies = mock(DocumentWorkingCopyRepository.class);
        DocumentRevisionRepository revisions = mock(DocumentRevisionRepository.class);
        DocumentPublicationRepository publications = mock(DocumentPublicationRepository.class);
        DocumentEntity document = new DocumentEntity(documentId, owner, resourceId, DocumentKind.DOCUMENT, null, now, now, 0L);
        DocumentWorkingCopyEntity copy = new DocumentWorkingCopyEntity(UUID.randomUUID(), documentId, owner, "current", "v1", null, now, 2L);
        DocumentRevisionEntity old = new DocumentRevisionEntity(UUID.randomUUID(), documentId, owner, 1L, "restored", "v1", now, owner);
        DocumentRevisionEntity created = new DocumentRevisionEntity(UUID.randomUUID(), documentId, owner, 2L, "restored", "v1", now, owner);
        when(documents.findById(documentId)).thenReturn(Mono.just(document));
        when(revisions.findAllByDocumentIdOrderByRevisionNumberDesc(documentId)).thenReturn(Flux.just(old));
        when(revisions.findTopByDocumentIdOrderByRevisionNumberDesc(documentId)).thenReturn(Mono.just(old));
        when(revisions.save(any())).thenReturn(Mono.just(created));
        when(documents.save(any())).thenReturn(Mono.just(document));
        when(copies.findByDocumentId(documentId)).thenReturn(Mono.just(copy));
        when(copies.save(any())).thenReturn(Mono.just(copy));

        StepVerifier.create(new PersistentDocumentService(resources, documents, copies, revisions, publications)
                .restore(owner, documentId, 1L))
            .expectNextMatches(result -> result.revisionNumber() == 2L && result.content().equals("restored"))
            .verifyComplete();
        verify(documents).save(any());
        verify(copies).save(any());
    }

    @Test
    void publishesAnOwnedRevision() {
        UUID owner = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceService resources = mock(ResourceService.class);
        DocumentRepository documents = mock(DocumentRepository.class);
        DocumentWorkingCopyRepository copies = mock(DocumentWorkingCopyRepository.class);
        DocumentRevisionRepository revisions = mock(DocumentRevisionRepository.class);
        DocumentPublicationRepository publications = mock(DocumentPublicationRepository.class);
        DocumentEntity document = new DocumentEntity(documentId, owner, UUID.randomUUID(), DocumentKind.ARTICLE, null, now, now, 0L);
        DocumentRevisionEntity revision = new DocumentRevisionEntity(UUID.randomUUID(), documentId, owner, 1L, "published", "v1", now, owner);
        when(documents.findById(documentId)).thenReturn(Mono.just(document));
        when(revisions.findAllByDocumentIdOrderByRevisionNumberDesc(documentId)).thenReturn(Flux.just(revision));
        when(publications.findByDocumentId(documentId)).thenReturn(Mono.empty());
        when(publications.save(any())).thenReturn(Mono.just(new DocumentPublicationEntity(UUID.randomUUID(), documentId, revision.id(), "hello", DocumentPublicationState.PUBLISHED, now, 0L)));

        StepVerifier.create(new PersistentDocumentService(resources, documents, copies, revisions, publications)
                .publish(owner, documentId, new PublishDocumentRequest("hello", 1L)))
            .expectNextMatches(publication -> publication.state() == DocumentPublicationState.PUBLISHED && publication.revisionId().equals(revision.id()))
            .verifyComplete();
    }
}
