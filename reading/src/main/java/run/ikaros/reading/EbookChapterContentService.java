package run.ikaros.reading;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface EbookChapterContentService {
    Mono<EbookChapterContentView> get(UUID ownerId, UUID importId, UUID chapterId);
}
