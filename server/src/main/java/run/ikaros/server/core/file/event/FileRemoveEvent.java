package run.ikaros.server.core.file.event;

import java.time.Clock;
import run.ikaros.server.store.entity.FileEntity;

public class FileRemoveEvent extends FileChangeEvent {

    public FileRemoveEvent(Object source, FileEntity fileEntity) {
        super(source, fileEntity);
    }

    public FileRemoveEvent(Object source, Clock clock, FileEntity fileEntity) {
        super(source, clock, fileEntity);
    }
}
