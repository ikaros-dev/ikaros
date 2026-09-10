package run.ikaros.reading;

import java.util.UUID;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.storage.api.AttachmentContentService;

@Service
public class PersistentReadingPageService implements ReadingPageService {
    private final ReadingChapterRepository chapters;
    private final ReadingComicPageRepository pages;
    private final AttachmentContentService attachments;

    public PersistentReadingPageService(ReadingChapterRepository chapters, ReadingComicPageRepository pages,
        AttachmentContentService attachments) {
        this.chapters = chapters; this.pages = pages; this.attachments = attachments;
    }

    @Override public Flux<ComicPageView> list(UUID ownerId, UUID chapterId) {
        return chapters.findById(chapterId).filter(chapter -> ownerId.equals(chapter.ownerId()))
            .switchIfEmpty(Mono.error(new NotFoundException("漫画章节不存在或无权访问")))
            .flatMapMany(ignored -> pages.findAllByChapterIdOrderByPageOrderAsc(chapterId).map(this::view));
    }

    @Override public Flux<DataBuffer> content(UUID ownerId, UUID pageId) {
        return pages.findById(pageId).switchIfEmpty(Mono.error(new NotFoundException("漫画页面不存在")))
            .flatMapMany(page -> chapters.findById(page.chapterId())
                .filter(chapter -> ownerId.equals(chapter.ownerId()))
                .switchIfEmpty(Mono.error(new NotFoundException("漫画页面不存在或无权访问")))
                .thenMany(attachments.read(ownerId, page.attachmentId())));
    }

    private ComicPageView view(ReadingComicPageEntity page) {
        return new ComicPageView(page.id(), page.chapterId(), page.attachmentId(), page.pageOrder(),
            page.pageRole(), page.width(), page.height(), page.spreadHint());
    }
}
