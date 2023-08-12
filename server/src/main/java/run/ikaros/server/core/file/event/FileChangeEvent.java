package run.ikaros.server.core.file.event;

import java.time.Clock;
import org.springframework.context.ApplicationEvent;
import run.ikaros.server.store.entity.FileEntity;

public class FileChangeEvent extends ApplicationEvent {
    private final FileEntity fileEntity;

    public FileChangeEvent(Object source, FileEntity fileEntity) {
        super(source);
        this.fileEntity = fileEntity;
    }

    public FileChangeEvent(Object source, Clock clock, FileEntity fileEntity) {
        super(source, clock);
        this.fileEntity = fileEntity;
    }

    public FileEntity getFileEntity() {
        return fileEntity;
    }
}
