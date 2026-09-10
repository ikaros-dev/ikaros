package run.ikaros.reading;

import java.util.UUID;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

public interface ReadingPageService {
    Flux<ComicPageView> list(UUID ownerId, UUID chapterId);
    Flux<DataBuffer> content(UUID ownerId, UUID pageId);
}
