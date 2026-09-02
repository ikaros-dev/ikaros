package run.ikaros.document;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentDocumentCommentService implements DocumentCommentService {
    private final DocumentRepository documents;
    private final DocumentRevisionRepository revisions;
    private final DocumentCommentRepository comments;

    public PersistentDocumentCommentService(DocumentRepository documents, DocumentRevisionRepository revisions,
        DocumentCommentRepository comments) {
        this.documents = documents;
        this.revisions = revisions;
        this.comments = comments;
    }

    @Override
    public Mono<DocumentCommentView> create(UUID ownerId, UUID documentId, CreateDocumentCommentRequest request) {
        Mono<DocumentRevisionEntity> anchor = request.anchorRevisionId() == null
            ? Mono.empty()
            : revisions.findById(request.anchorRevisionId()).filter(revision -> revision.documentId().equals(documentId))
                .switchIfEmpty(Mono.error(new NotFoundException("Anchor Revision 不属于该 Document")));
        return ownedDocument(ownerId, documentId)
            .then(anchor.defaultIfEmpty(new DocumentRevisionEntity(null, documentId, ownerId, 0, "", "", Instant.now(), ownerId)))
            .flatMap(ignored -> comments.save(new DocumentCommentEntity(null, documentId, ownerId,
                request.parentCommentId(), request.anchorRevisionId(), request.anchor(), request.body(),
                DocumentCommentStatus.OPEN, Instant.now(), Instant.now(), null)))
            .map(this::view);
    }

    @Override
    public Flux<DocumentCommentView> list(UUID ownerId, UUID documentId) {
        return ownedDocument(ownerId, documentId).flatMapMany(ignored ->
            comments.findAllByDocumentIdAndStatusNotOrderByCreatedAtAsc(documentId, DocumentCommentStatus.DELETED)
                .map(this::view));
    }

    @Override
    public Mono<DocumentCommentView> resolve(UUID ownerId, UUID commentId) {
        return ownedComment(ownerId, commentId)
            .flatMap(comment -> comments.save(new DocumentCommentEntity(comment.id(), comment.documentId(),
                comment.authorId(), comment.parentCommentId(), comment.anchorRevisionId(), comment.anchor(), comment.body(),
                DocumentCommentStatus.RESOLVED, comment.createdAt(), Instant.now(), comment.version())))
            .map(this::view);
    }

    @Override
    public Mono<Void> delete(UUID ownerId, UUID commentId) {
        return ownedComment(ownerId, commentId)
            .flatMap(comment -> comments.save(new DocumentCommentEntity(comment.id(), comment.documentId(),
                comment.authorId(), comment.parentCommentId(), comment.anchorRevisionId(), comment.anchor(), comment.body(),
                DocumentCommentStatus.DELETED, comment.createdAt(), Instant.now(), comment.version())))
            .then();
    }

    private Mono<DocumentEntity> ownedDocument(UUID ownerId, UUID documentId) {
        return documents.findById(documentId).filter(document -> document.ownerId().equals(ownerId))
            .switchIfEmpty(Mono.error(new NotFoundException("Document 不存在或无权访问")));
    }

    private Mono<DocumentCommentEntity> ownedComment(UUID ownerId, UUID commentId) {
        return comments.findById(commentId)
            .flatMap(comment -> ownedDocument(ownerId, comment.documentId()).thenReturn(comment))
            .switchIfEmpty(Mono.error(new NotFoundException("Comment 不存在或无权访问")));
    }

    private DocumentCommentView view(DocumentCommentEntity comment) {
        return new DocumentCommentView(comment.id(), comment.documentId(), comment.authorId(), comment.parentCommentId(),
            comment.anchorRevisionId(), comment.anchor(), comment.body(), comment.status(), comment.createdAt(), comment.updatedAt());
    }
}
