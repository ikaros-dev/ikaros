package run.ikaros.server.search.file;

import java.util.List;
import java.util.Set;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.search.file.FileDoc;
import run.ikaros.api.search.file.FileHint;
import run.ikaros.api.search.file.FileSearchService;
import run.ikaros.api.store.entity.FileEntity;
import run.ikaros.server.core.file.event.FileAddEvent;
import run.ikaros.server.core.file.event.FileRemoveEvent;

@Component
public class FileEventListener {

    private final FileSearchService fileSearchService;

    public FileEventListener(FileSearchService fileSearchService) {
        this.fileSearchService = fileSearchService;
    }

    /**
     * {@link FileAddEvent} listener.
     */
    @EventListener(FileAddEvent.class)
    public Mono<Void> handleFileAddEvent(FileAddEvent event) throws Exception {
        FileEntity entity = event.getFileEntity();
        FileDoc fileDoc = new FileDoc();
        fileDoc.setName(entity.getName());
        fileDoc.setType(entity.getType());
        fileDoc.setPlace(entity.getPlace());
        fileDoc.setUrl(entity.getUrl());
        fileSearchService.addDocuments(List.of(fileDoc));
        return Mono.empty();
    }

    /**
     * {@link FileRemoveEvent} listener.
     */
    @EventListener(FileRemoveEvent.class)
    public Mono<Void> handleFileRemoveEvent(FileRemoveEvent event) throws Exception {
        FileEntity entity = event.getFileEntity();
        FileHint fileDoc = new FileHint(entity.getName(), entity.getOriginalPath(), entity.getUrl(),
            entity.getType(), entity.getPlace());
        fileSearchService.removeDocuments(Set.of(fileDoc.name()));
        return Mono.empty();
    }

}
