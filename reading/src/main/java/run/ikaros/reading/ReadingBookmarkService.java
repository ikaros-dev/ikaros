package run.ikaros.reading;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReadingBookmarkService {
    Mono<ReadingBookmarkView> create(UUID ownerId, UUID workId, UUID editionId, UUID chapterId,
        CreateReadingBookmarkRequest request);
    Flux<ReadingBookmarkView> list(UUID ownerId, UUID workId);
    Mono<Void> delete(UUID ownerId, UUID bookmarkId);
}
