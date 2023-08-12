package run.ikaros.server.search.file;

import java.util.List;
import java.util.Set;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.search.file.FileDoc;
import run.ikaros.api.search.file.FileSearchService;
import run.ikaros.server.core.file.event.FileRemoveEvent;
import run.ikaros.server.core.file.event.FileSaveEvent;
import run.ikaros.server.store.entity.FileEntity;

@Component
public class FileEventListener {

    private final FileSearchService fileSearchService;

    public FileEventListener(FileSearchService fileSearchService) {
        this.fileSearchService = fileSearchService;
    }

    /**
     * {@link FileSaveEvent} listener.
     */
    @EventListener(FileSaveEvent.class)
    public Mono<Void> handleFileAddEvent(FileSaveEvent event) throws Exception {
        FileEntity entity = event.getFileEntity();
        FileDoc fileDoc = FileDocConverter.fromEntity(entity);
        fileSearchService.updateDocument(List.of(fileDoc));
        return Mono.empty();
    }

    /**
     * {@link FileRemoveEvent} listener.
     */
    @EventListener(FileRemoveEvent.class)
    public Mono<Void> handleFileRemoveEvent(FileRemoveEvent event) throws Exception {
        FileEntity entity = event.getFileEntity();
        fileSearchService.removeDocuments(Set.of(String.valueOf(entity.getId())));
        return Mono.empty();
    }

}
