package run.ikaros.reading;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentReadingBookmarkService implements ReadingBookmarkService {
    private final ReadingBookmarkRepository bookmarks;
    private final ReadingWorkRepository works;
    private final ReadingEditionRepository editions;
    private final ReadingChapterRepository chapters;

    public PersistentReadingBookmarkService(ReadingBookmarkRepository bookmarks, ReadingWorkRepository works,
                                            ReadingEditionRepository editions, ReadingChapterRepository chapters) {
        this.bookmarks = bookmarks; this.works = works; this.editions = editions; this.chapters = chapters;
    }

    @Override
    public Mono<ReadingBookmarkView> create(UUID ownerId, UUID workId, UUID editionId, UUID chapterId,
                                            CreateReadingBookmarkRequest request) {
        return ownedWork(ownerId, workId)
            .then(editions.findById(editionId).filter(e -> e.ownerId().equals(ownerId) && e.workId().equals(workId))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new NotFoundException("Edition 不存在或无权访问")))))
            .then(chapters.findById(chapterId).filter(c -> c.ownerId().equals(ownerId)
                    && c.workId().equals(workId) && c.editionId().equals(editionId))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new NotFoundException("Chapter 不存在或无权访问")))))
            .then(Mono.defer(() -> bookmarks.save(new ReadingBookmarkEntity(null, ownerId, workId, editionId,
                chapterId, request.locatorKind().trim(), request.locatorValue().trim(), request.contentVersion(),
                request.label(), Instant.now()))))
            .map(this::view)
            .onErrorMap(error -> error.getCause() instanceof org.springframework.dao.DataIntegrityViolationException,
                error -> new ConflictException("该章节定位点已存在书签"));
    }

    @Override
    public Flux<ReadingBookmarkView> list(UUID ownerId, UUID workId) {
        return ownedWork(ownerId, workId).thenMany(bookmarks.findAllByOwnerIdAndWorkIdOrderByCreatedAtDesc(ownerId, workId).map(this::view));
    }

    @Override
    public Mono<Void> delete(UUID ownerId, UUID bookmarkId) {
        return bookmarks.findById(bookmarkId).filter(b -> b.ownerId().equals(ownerId))
            .switchIfEmpty(Mono.defer(() -> Mono.error(new NotFoundException("书签不存在或无权访问"))))
            .flatMap(bookmarks::delete);
    }

    private Mono<ReadingWorkEntity> ownedWork(UUID ownerId, UUID workId) {
        return works.findById(workId).filter(w -> w.ownerId().equals(ownerId))
            .switchIfEmpty(Mono.defer(() -> Mono.error(new NotFoundException("Work 不存在或无权访问"))));
    }

    private ReadingBookmarkView view(ReadingBookmarkEntity value) {
        return new ReadingBookmarkView(value.id(), value.workId(), value.editionId(), value.chapterId(),
            value.locatorKind(), value.locatorValue(), value.contentVersion(), value.label(), value.createdAt());
    }
}
