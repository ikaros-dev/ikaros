package run.ikaros.server.core.file.event;

import org.springframework.context.ApplicationEvent;
import run.ikaros.api.store.entity.FileEntity;

public class FileAddEvent extends ApplicationEvent {
    private final FileEntity fileEntity;

    public FileAddEvent(Object source, FileEntity fileEntity) {
        super(source);
        this.fileEntity = fileEntity;
    }

    public FileEntity getFileEntity() {
        return fileEntity;
    }
}
